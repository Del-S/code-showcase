    .....
    
    override fun qrCodeDetectionFail() {
        event.postValue(QrPaymentEvent.QrCodeDetectionFail)
    }

    fun chooseFromGallery() {
        logUserAction(LOG_DOMESTIC_PAYMENT) { "Choose image (QR code) from gallery" }
        event.value = QrPaymentEvent.PickupImage
    }

    fun loadFromPdf() {
        logUserAction(LOG_DOMESTIC_PAYMENT) { "Pickup QR code from PDF" }
        event.value = QrPaymentEvent.PickupPdf
    }

    /**
     * Decodes PDF and tries to detect QR on any page of the PDF. First QR code is taken and
     * decoding finishes. Searches through all PDF pages if it is needed. Parses PDF file into
     * Bitmap and send it to the [decodeBitmap] function for decoding.
     *
     * @param fileDescriptor of the PDF file
     * @see decodeBitmap
     */
    fun decodePdf(fileDescriptor: ParcelFileDescriptor) {
        val pdfRenderer = PdfRenderer(fileDescriptor)
        viewModelScope.launch(Dispatchers.IO) {
            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                if (handleDetectQr(bitmap, pdfRenderer.pageCount > 1)) {
                    page.close()
                    pdfRenderer.close()
                    return@launch
                }
                page.close()
            }
            qrCodeDetectionFail()
        }
    }

    /**
     * Decodes bitmap and tries to find QR code in it. See [detectQr] for more information.
     *
     * @param bitmap to be searched for QR
     * @param blocking true for blocking and false for asynchronous
     */
    fun decodeBitmap(bitmap: Bitmap, blocking: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            handleDetectQr(bitmap, blocking)
        }
    }

    /**
     * Handles QR detection in bitmap. Detection can be asynchronous or blocking based on the
     * [blocking] parameter. Calls [detectQrBlocking] for blocking and [detectQrAsync] for
     * asynchronous detection.
     *
     * @param bitmap to be searched for QR
     * @param blocking true for blocking and false for asynchronous
     * @return true if detection found qr else false (async always returns true)
     */
    private fun handleDetectQr(bitmap: Bitmap, blocking: Boolean = false): Boolean {
        logVerbose("CameraPaymentFragment().ProcessPickedImage().bitmap = $bitmap")
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()

        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        val visionImage = FirebaseVisionImage.fromBitmap(bitmap)
        return if (blocking) {
            detectQrBlocking(detector, visionImage)
        } else {
            detectQrAsync(detector, visionImage)
            true
        }
    }

    /**
     * Decodes QR synchronously. Checks first barCode and if it is not empty then it
     * posts it to processing using function [qrCodeDetected]. Does not send fail event thus
     * multiple images can be checked.
     *
     * @param detector used to detect bar codes
     * @param visionImage image being searched for bar codes
     * @return true if qr code was detected else false
     */
    private fun detectQrBlocking(
        detector: FirebaseVisionBarcodeDetector,
        visionImage: FirebaseVisionImage
    ) = Tasks.await(detector.detectInImage(visionImage))?.getOrNull(0)?.rawValue?.let {
        qrCodeDetected(it)
        true
    } ?: false

    /**
     * Decodes QR asynchronously using Task. Checks first barCode and if it is not empty then it
     * posts it to processing using function [qrCodeDetected]. On any error or if bar code is
     * missing error is posted by [qrCodeDetectionFail].
     *
     * @param detector used to detect bar codes
     * @param visionImage image being searched for bar codes
     */
    private fun detectQrAsync(
        detector: FirebaseVisionBarcodeDetector,
        visionImage: FirebaseVisionImage
    ) {
        detector.detectInImage(visionImage)
            .addOnSuccessListener { barCodes ->
                barCodes.getOrNull(0)?.rawValue?.let {
                    qrCodeDetected(it)
                } ?: qrCodeDetectionFail()
            }
            .addOnFailureListener {
                logError("something went wrong", it)
                qrCodeDetectionFail()
            }
    }

    private suspend fun processParsingResult(parsingResult: Result<PaymentRequest>) {
        when (parsingResult) {
            is Result.Success -> {
                savePaymentRequestData(parsingResult.data)
                event.postValue(
                    QrPaymentEvent.Success(
                        savedTransactionId = parsingResult.data.creationTime
                    )
                )
            }
            is Result.Error -> {
                event.postValue(
                    QrPaymentEvent.Error(msg = parsingResult.error.message ?: "")
                )
            }
        }
    }
    
    ....
