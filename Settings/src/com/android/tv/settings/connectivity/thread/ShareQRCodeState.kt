/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.settings.connectivity.thread

import android.content.Context
import android.net.thread.ThreadNetworkController
import android.net.thread.ThreadNetworkException
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.OutcomeReceiver
import android.util.Log
import androidx.core.text.HtmlCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.tv.settings.R
import com.android.tv.settings.connectivity.util.State
import com.android.tv.settings.connectivity.util.StateMachine
import com.android.tv.settings.connectivity.util.ThreadNetworkHelper
import com.android.tv.twopanelsettings.QrCodeView
import java.time.Duration
import java.time.Instant

class ShareQRCodeState(private val activity: ShareThreadNetworkActivity) : State {
    private var fragment : Fragment = ShareQRCodeFragment()
    private val stateMachine = ViewModelProvider(activity)[StateMachine::class.java]

    override fun processForward() {
        fragment = ShareQRCodeFragment()
        activity.onFragmentChange(fragment, true)
    }

    override fun processBackward() {
        fragment = ShareQRCodeFragment()
        stateMachine.back()
    }

    override fun getFragment(): Fragment = fragment

    class ShareQRCodeFragment : Fragment() {
        private lateinit var stateMachine: StateMachine
        private lateinit var threadNetworkHelper: ThreadNetworkHelper
        private var timeoutMinutes : Int = 0
        private lateinit var qrCodeView: QrCodeView
        private lateinit var qrCodeString: TextView
        private lateinit var qrCodeCountdown: TextView
        private var ephemeralKey: String? = null
        private var ephemeralKeyExpiry: Instant? = null
        private var countDownTimer: CountDownTimer? = null
        private var activatingEphemeralMode : Boolean = false
        private var successShown = false
        private var remainingMillis: Long = 0

        private val onStateChangeListener = object : ThreadNetworkHelper.OnStateChangeListener {
            override fun onEphemeralKeyStateChanged(
                state: Int,
                key: String?,
                expiry: Instant?
            ) {
                ephemeralKey = key
                ephemeralKeyExpiry = expiry
                when (state) {
                    ThreadNetworkController.EPHEMERAL_KEY_ENABLED -> {
                        updateQrCode(key!!)
                        val remaining = Duration.between(Instant.now(), expiry)
                        countDownTimer?.cancel()

                        if (remaining.isNegative) {
                            qrCodeCountdown.text = ""
                        } else {
                            val minutes = remaining.toMinutes()
                            val seconds = remaining.minusMinutes(minutes).seconds
                            setCountdownText(minutes, seconds)

                            countDownTimer =
                                object : CountDownTimer(remaining.toMillis(), 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        remainingMillis = millisUntilFinished
                                        val remaining = Duration.ofMillis(millisUntilFinished)
                                        val minutes = remaining.toMinutes()
                                        val seconds =
                                            remaining.minusMinutes(minutes).seconds
                                        setCountdownText(minutes, seconds)
                                    }

                                    override fun onFinish() {
                                        remainingMillis = 0
                                        maybeReactivateEphemeralKey()
                                    }
                                }.start()
                        }
                    }
                    ThreadNetworkController.EPHEMERAL_KEY_IN_USE -> {
                        if (successShown) return
                        successShown = true

                        Toast.makeText(
                            requireContext(),
                            R.string.thread_network_sharing_success,
                            Toast.LENGTH_SHORT
                        ).show()
                        requireActivity().finish()
                    }
                    ThreadNetworkController.EPHEMERAL_KEY_DISABLED -> {
                        if (countDownTimer != null && remainingMillis > TIMER_FIRE_THRESHOLD_MS) {
                            stateMachine.listener?.onComplete(
                                this@ShareQRCodeFragment,
                                StateMachine.RESULT_FAILURE
                            )
                        }
                    }
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            stateMachine = ViewModelProvider(requireActivity())[StateMachine::class.java]
            threadNetworkHelper = ThreadNetworkHelper.getInstance(requireContext())
            timeoutMinutes = resources.getInteger(
                R.integer.share_thread_network_key_validity_minutes)
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.share_thread_network_qr_code, container, false)
            qrCodeView = view.findViewById(R.id.setup_qrcode_view)
            qrCodeString = view.findViewById(R.id.thread_qr_code_string)
            qrCodeCountdown = view.findViewById(R.id.thread_qr_code_countdown)
            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val description = view.requireViewById<TextView>(R.id.setup_qrcode_description)
            description.text = getString(R.string.share_thread_description, Build.MODEL)
            threadNetworkHelper.setOnStateChangeListener(onStateChangeListener)
            threadNetworkHelper.registerStateCallback()
            activateEphemeralKey()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            countDownTimer?.cancel()
            threadNetworkHelper.setOnStateChangeListener(null)
            threadNetworkHelper.unregisterStateCallback()
            deactivateEphemeralKey()
        }

        private fun deactivateEphemeralKey() {
            threadNetworkHelper.deactivateEphimeralKeyCode(
                object : OutcomeReceiver<Void, ThreadNetworkException> {
                    override fun onResult(result: Void?) {}
                    override fun onError(error: ThreadNetworkException) {}
                })
        }

        private fun maybeReactivateEphemeralKey() {
            if (activatingEphemeralMode) {
                return
            }

            val reauthExpired = ShareThreadNetworkActivity.isReauthExpired(requireContext())

            if (resources.getBoolean(R.bool.config_share_thread_network_key_refreshes) &&
                !reauthExpired
            ) {
                activateEphemeralKey()
            } else {
                if (reauthExpired) {
                    Log.i(TAG, "Reauth has expired, not refreshing QR code.")
                }
                requireActivity().finish()
            }
        }

        private fun activateEphemeralKey() {
            // Deactivate first because otherwise we can get already activated error.
            qrCodeView.setData(null)
            qrCodeString.text = ""
            qrCodeCountdown.text = ""
            ephemeralKey = null
            ephemeralKeyExpiry = null

            activatingEphemeralMode = true;

            threadNetworkHelper.activateEphimeralKeyMode(
                Duration.ofMinutes(timeoutMinutes.toLong()),
                object : OutcomeReceiver<Void, ThreadNetworkException> {
                    override fun onResult(result: Void?) {
                        activatingEphemeralMode = false
                        Log.i(TAG, "Ephemeral mode activated")
                    }

                    override fun onError(e : ThreadNetworkException) {
                        activatingEphemeralMode = false
                        Log.e(TAG, "Failed to activate ephemeral mode", e)
                        stateMachine.listener?.onComplete(this@ShareQRCodeFragment,
                            StateMachine.RESULT_FAILURE)
                    }
                }
            )
        }

        private fun updateQrCode(key: String) {
            qrCodeView.setData(key)
            qrCodeString.text = key.chunked(3).joinToString(" - ")
        }

        private fun setCountdownText(minutes: Long, seconds: Long) {
            val countdownText = getString(
                R.string.share_thread_network_countdown,
                minutes,
                seconds
            )
            qrCodeCountdown.text =
                HtmlCompat.fromHtml(countdownText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    companion object {
        const val TAG = "ThreadShareQRCode"
        private const val TIMER_FIRE_THRESHOLD_MS = 1000
    }
}
