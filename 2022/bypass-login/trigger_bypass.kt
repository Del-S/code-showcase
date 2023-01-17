    @MainThread
    fun onConfirmSetupClick(view: View) {
        viewModelScope.launch {
            updateActiveEnvironment(getSelectedEnvironment())
            updateFeatureFlagsConfig(featureFlags.value!!)
            KateConfig.isOnLocalhost = kateOnLocalhost.value!!

            loginToken.value.takeUnless(String?::isNullOrBlank)?.let { loginToken ->
                view.showSnackbar("Bypassing Login. Please wait!", Snackbar.LENGTH_SHORT)
                triggerBypassLogin(view, loginToken)
            } ?: run {
                setBypassLoginToken(SetBypassLoginTokenUseCase.Params(null))
                _flowEvent.value = DebugSetupFlowEvent.SetupFinished
            }
        }
    }
    
    /**
     * Triggers login bypass by bypassing login and then loading all required data for the app to
     * function such as token metadata, global settings and more. If all data are loaded then
     * application directs the user to Dashboard. Else it starts default flow (tutorial, fingerboard
     * and login).
     *
     * Note: it is run on NonCancellable context to prevent cancellation due to CsobErrorInterceptor
     * which cancels coroutineContext when critical error code is returned. So for example when
     * token metadata call would return 405 (invalid access token) it would cancel this coroutine
     * and stopped execution, thus nothing would happen.
     *
     * Warning: [BypassLoginFetchRequiredDataUseCase] must be injected directly using
     * getKoin().get() because bypass UC refreshes scope to provide new certificates for Retrofit.
     * This allows certificate modification for SSL authorization. If it would not be done like this
     * it would use old certificate.
     *
     * @param view used to show snackbar to the user
     * @param loginToken contains access token, cert and other values to be used in bypass
     * @see BypassLoginUseCase
     * @see BypassLoginFetchRequiredDataUseCase
     */
    private suspend fun triggerBypassLogin(view: View, loginToken: String) =
        withContext(Dispatchers.Default + NonCancellable) {
            bypassLogin(BypassLoginUseCase.Params(loginToken)).chain {
                getKoin().getScope(SCOPE_RETROFIT_ID).get<BypassLoginFetchRequiredDataUseCase>()()
            }.onSuccess {
                logVerbose { "Bypass login success" }
                _flowEvent.postValue(DebugSetupFlowEvent.BypassLogin)
            }.onError {
                view.showSnackbar("Bypass login error!", Snackbar.LENGTH_SHORT)
                logVerbose { "Bypass login failed $it" }
            }
        }
