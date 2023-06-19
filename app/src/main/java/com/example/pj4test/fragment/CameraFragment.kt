/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pj4test.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.example.pj4test.ProjectConfiguration
import com.example.pj4test.R
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.pj4test.cameraInference.PersonClassifier
import com.example.pj4test.databinding.FragmentCameraBinding
import com.example.pj4test.ml.EfficientnetLite0Fp322
import org.tensorflow.lite.task.vision.detector.Detection
import com.example.pj4test.databinding.FragmentAudioBinding
import com.example.pj4test.fragment.GlobalVariables
import java.time.LocalDateTime
import java.util.Calendar
import java.time.Duration

class CameraFragment : Fragment(), PersonClassifier.DetectorListener {
    private val TAG = "CameraFragment"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!
    
    private lateinit var personView: TextView
    private lateinit var statsView: TextView
    
    private lateinit var personClassifier: PersonClassifier
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    var have_seen_the_dog = false
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    var lastUpdateTime: Long = System.currentTimeMillis()// The timestamp of the last variable update
    var petDetectionStartTime = System.currentTimeMillis()
    val dogList = listOf(
        "Chihuahua",
        "Japanese spaniel",
        "Maltese dog, Maltese terrier, Maltese",
        "Pekinese, Pekingese, Peke",
        "Shih-Tzu",
        "Blenheim spaniel",
        "papillon",
        "toy terrier",
        "Rhodesian ridgeback",
        "Afghan hound, Afghan",
        "basset, basset hound",
        "beagle",
        "bloodhound, sleuthhound",
        "bluetick",
        "black-and-tan coonhound",
        "Walker hound, Walker foxhound",
        "English foxhound",
        "redbone",
        "borzoi, Russian wolfhound",
        "Irish wolfhound",
        "Italian greyhound",
        "whippet",
        "Ibizan hound, Ibizan Podenco",
        "Norwegian elkhound, elkhound",
        "otterhound, otter hound",
        "Saluki, gazelle hound",
        "Scottish deerhound, deerhound",
        "Weimaraner",
        "Staffordshire bullterrier, Staffordshire bull terrier",
        "American Staffordshire terrier, Staffordshire terrier, American pit bull terrier, pit bull terrier",
        "Bedlington terrier",
        "Border terrier",
        "Kerry blue terrier",
        "Irish terrier",
        "Norfolk terrier",
        "Norwich terrier",
        "Yorkshire terrier",
        "wire-haired fox terrier",
        "Lakeland terrier",
        "Sealyham terrier, Sealyham",
        "Airedale, Airedale terrier",
        "cairn, cairn terrier",
        "Australian terrier",
        "Dandie Dinmont, Dandie Dinmont terrier",
        "Boston bull, Boston terrier",
        "miniature schnauzer",
        "giant schnauzer",
        "standard schnauzer",
        "Scotch terrier, Scottish terrier, Scottie",
        "Tibetan terrier, chrysanthemum dog",
        "silky terrier, Sydney silky",
        "soft-coated wheaten terrier",
        "West Highland white terrier",
        "Lhasa, Lhasa apso",
        "flat-coated retriever",
        "curly-coated retriever",
        "golden retriever",
        "Labrador retriever",
        "Chesapeake Bay retriever",
        "German short-haired pointer",
        "vizsla, Hungarian pointer",
        "English setter",
        "Irish setter, red setter",
        "Gordon setter",
        "Brittany spaniel",
        "clumber, clumber spaniel",
        "English springer, English springer spaniel",
        "Welsh springer spaniel",
        "cocker spaniel, English cocker spaniel, cocker",
        "Sussex spaniel",
        "Irish water spaniel",
        "kuvasz",
        "schipperke",
        "groenendael",
        "malinois",
        "briard",
        "kelpie",
        "komondor",
        "Old English sheepdog, bobtail",
        "Shetland sheepdog, Shetland sheep dog, Shetland",
        "collie",
        "Border collie",
        "Bouvier des Flandres, Bouviers des Flandres",
        "Rottweiler",
        "German shepherd, German shepherd dog, German police dog, alsatian",
        "Doberman, Doberman pinscher",
        "miniature pinscher",
        "Greater Swiss Mountain dog",
        "Bernese mountain dog",
        "Appenzeller",
        "EntleBucher",
        "boxer",
        "bull mastiff",
        "Tibetan mastiff",
        "French bulldog",
        "Great Dane",
        "Saint Bernard, St Bernard",
        "Eskimo dog, husky",
        "malamute, malemute, Alaskan malamute",
        "Siberian husky",
        "dalmatian, coach dog, carriage dog",
        "affenpinscher, monkey pinscher, monkey dog",
        "basenji",
        "pug, pug-dog",
        "Leonberg",
        "Newfoundland, Newfoundland dog",
        "Great Pyrenees",
        "Samoyed, Samoyede",
        "Pomeranian",
        "chow, chow chow"
    )
    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        personClassifier = PersonClassifier()
        personClassifier.initialize(requireContext())
        personClassifier.setDetectorListener(this)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        personView = fragmentCameraBinding.PersonView
        statsView = fragmentCameraBinding.StatsView

    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                val cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases(cameraProvider)
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()
        // Attach the viewfinder's surface provider to preview use case
        preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)


        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
        // The analyzer can then be assigned to the instance
        imageAnalyzer!!.setAnalyzer(cameraExecutor) { image -> detectObjects(image) }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }
    private fun isWithinTimeRange(): Boolean {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        val startHour = 7
        val endHour = 21

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        Log.d("APP", "Time: $currentHour")


        return currentHour in startHour..endHour
    }

    private fun detectObjects(image: ImageProxy) {
        if (!isWithinTimeRange()) {
            // Do not perform inference outside the specified time range
            personView.text = "Good night!"
            return
        }
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            bitmapBuffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        val imageRotation = image.imageInfo.rotationDegrees

        // Pass Bitmap and rotation to the object detector helper for processing and detection
        personClassifier.detect(bitmapBuffer, imageRotation)

    }


    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView

    override fun onObjectDetectionResults(
        results: MutableList<Detection>?,
        outputs: EfficientnetLite0Fp322.Outputs,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )
            val probability = outputs.probabilityAsCategoryList
            val top5Probabilities = probability.sortedByDescending { it.score }.take(2)
            // Log.d("APP", top5Probabilities.toString())
            var isPetDetected = false

            for (result in top5Probabilities) {
                if (dogList.any { it.contains(result.label, ignoreCase = true) }) {
                    Log.d("APP", "Label: ${result.label}, Probability: ${result.score}")
                    isPetDetected = true
                }
            }


            // Log.d("APP", "isPetdetected: ${isPetDetected}")

            for (result in top5Probabilities) {
                if (dogList.any { result.label.contains(it, ignoreCase = true) }) {
                    //Log.d("APP", "Label: ${result.label}, Probability: ${result.score}, isPetdetected: ${isPetDetected}")
                }
            }
            // find at least one bounding box of the person
            val isPersonDetected: Boolean = results!!.find { it.categories[0].label == "tv" } != null
            var heard_sounds = false

            var currentTime = System.currentTimeMillis()
            var elapsedTime = currentTime - petDetectionStartTime
            var isPetInFrameForOneMinute = elapsedTime >= 6000


            // change UI according to the result
            if (isPetDetected) {
                if (GlobalVariables.myGlobalVariable == "Sounds!" && !heard_sounds)  {
                    heard_sounds = true
                }
                personView.text = "A pet detected!"


                var stats = statsView.text
                var lines = stats.split("\n")
                var fed = lines[0].split(": ")[1].toInt()
                var asked = lines[1].split(": ")[1].toInt()
                // Log.d("APP2", "Fed, asked: ${fed}, ${asked}")

                Log.d("APP22", have_seen_the_dog.toString())

                personView.setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                personView.setTextColor(ProjectConfiguration.activeTextColor)


                if (isPetInFrameForOneMinute and heard_sounds) {
                    if(!have_seen_the_dog) {
                        asked += 1
                        statsView.text = "№ fed: $fed\n№ asked: $asked"
                        have_seen_the_dog = true
                    }

                    if((fed <3)){

                        val currentTime = System.currentTimeMillis()
                        val timeDifference = currentTime - lastUpdateTime
//                        val sixHoursInMillis = 6 * 60 * 60 * 1000 // Conv
                        val sixHoursInMillis = 10 * 1000 // Conv


                        if((fed == 0 )or (timeDifference >= sixHoursInMillis)){
                            fed += 1
                            lastUpdateTime = System.currentTimeMillis()
                            //Log.d("APP2", checkIfSixHoursPassed(last_fed).toString())

                            personView.text = "Feeding the pet"
                            personView.setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                            personView.setTextColor(ProjectConfiguration.activeTextColor)
                            statsView.text = "№ fed: $fed\n№ asked: $asked"
                        }

                    }

                }

            } else {
                have_seen_the_dog = false
                petDetectionStartTime = System.currentTimeMillis()
                heard_sounds = false
                personView.text = "No pet here!"
                personView.setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                personView.setTextColor(ProjectConfiguration.idleTextColor)
            }

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onObjectDetectionError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}
