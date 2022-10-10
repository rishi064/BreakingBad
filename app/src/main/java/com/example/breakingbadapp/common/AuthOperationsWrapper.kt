package com.example.breakingbadapp.common

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthOperationsWrapper@Inject constructor(private val firebaseAuth: FirebaseAuth) {
    private var verificationId: String? = null
    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let {
                    onSuccess()
                }
            }.addOnFailureListener {
                onFailure(it.message.orEmpty())
            }
    }
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let {
                    onSuccess()
                }
            }.addOnFailureListener {
                onFailure(it.message.orEmpty())
            }
}
    fun sendVerificationCode(
        phoneNumber: String,
        onCodeSent: () -> Unit = {},
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential, {
                        onSuccess()
                    }, {
                        onFailure(it)
                    })
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (e is FirebaseAuthInvalidCredentialsException) {

                        onFailure("Invalid Request!")
                    } else if (e is FirebaseTooManyRequestsException) {
                        onFailure("Too many request!")
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent()
                    this@AuthOperationsWrapper.verificationId = verificationId
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(
        verifyCode: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        verificationId?.let { verifId ->
            val credential = PhoneAuthProvider.getCredential(verifId, verifyCode)
            signInWithCredential(credential, {
                onSuccess()
            }, {
                onFailure(it)
            })
        }
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        firebaseAuth.signInWithCredential(credential).addOnSuccessListener { authResult ->
            authResult.user?.let {
                onSuccess()
            }
        }.addOnFailureListener {
            onFailure(it.message.orEmpty())
        }
    }

    fun signInAnonymously(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        firebaseAuth.signInAnonymously().addOnSuccessListener { authResult ->
            authResult.user?.let {
                onSuccess()
            }
        }.addOnFailureListener {
            onFailure(it.message.orEmpty())
        }
    }
    fun checkCurrentUser(currentUser: (FirebaseUser) -> Unit = {}) {
        firebaseAuth.currentUser?.let {
            currentUser(it)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun signInWithGoogle(
        activity: Activity,
        oneTapClient: SignInClient,
        onSuccess: (IntentSenderRequest) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {

        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("327723893396-027qt4jp54l7o014efednnc1avn7gtmj.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(activity) { result ->
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender)
                            .build()
                    onSuccess(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    onFailure("Couldn't start One Tap UI: ${e.message}")
                }
            }
            .addOnFailureListener {
                onFailure(it.message.orEmpty())
            }
    }
    fun signInWithGithub(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("login", "")

        firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener { authResult ->
                authResult.user?.let {
                    onSuccess()
                }
            }
            .addOnFailureListener {
                onFailure(it.message.orEmpty())
            }
    }
}