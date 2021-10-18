package com.fintent.xintent.flutterx_intent

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** FlutterxIntentPlugin */
class FlutterxIntentPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private var activityCompletedCallBack: ActivityCompletedCallBack? = null
  lateinit var toBeCapturedImageLocationURI: Uri
  lateinit var tobeCapturedImageLocationFilePath: File

  private var activityAware: ActivityPluginBinding? = null
  private lateinit var channel: MethodChannel

  override fun onMethodCall(call: MethodCall, result: Result) {
    activityAware?.addActivityResultListener { requestCode, resultCode, intent ->
      when (requestCode) {
        999 -> {
          if (resultCode == Activity.RESULT_OK) {
            val filePaths = mutableListOf<String>()
            if (intent.clipData != null) {
              var i = 0
              while (i < intent.clipData?.itemCount!!) {
                if (intent.type == ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
                  filePaths.add(resolveContacts(intent.clipData?.getItemAt(i)?.uri!!))
                else
                  filePaths.add(uriToFilePath(intent.clipData?.getItemAt(i)?.uri!!))
                i++
              }
              activityCompletedCallBack?.sendDocument(filePaths)
            } else {
              if (intent.type == ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
                filePaths.add(resolveContacts(intent.data!!))
              else
                filePaths.add(uriToFilePath(intent.data!!))
              activityCompletedCallBack?.sendDocument(filePaths)
            }
            true
          } else {
            activityCompletedCallBack?.sendDocument(listOf())
            false
          }
        }
        998 -> {
          if (resultCode == Activity.RESULT_OK) {
            activityCompletedCallBack?.sendDocument(
              listOf(
                tobeCapturedImageLocationFilePath.absolutePath
              )
            )
            true
          } else
            false
        }
        else -> {
          false
        }
      }
    }

    when (call.method) {
      // when we're not interested in result of activity started, we call this method via platform channel
      "startActivity" -> {
        val intent = Intent()
        intent.action = call.argument<String>("action")
        if (call.argument<String>("package") != null)
          intent.`package` = call.argument<String>("package")
        if (call.argument<String>("data") != null)
          intent.data = Uri.parse(call.argument<String>("data"))

        // typeInfo parsed into associative array, which can be used for type casting extra data
        val typeInfo = call.argument<Map<String, String>>("typeInfo")

        call.argument<Map<String, Any>>("extra")?.apply {
          this.entries.forEach {
            when (it.key) {
              Intent.EXTRA_DONT_KILL_APP,
              Intent.EXTRA_LOCAL_ONLY,
              Intent.EXTRA_ALLOW_MULTIPLE, Intent.EXTRA_PROCESS_TEXT_READONLY -> intent.putExtra(
                it.key,
                it.value as Boolean
              )
              Intent.EXTRA_EMAIL,
              Intent.EXTRA_BCC,
              Intent.EXTRA_CC, Intent.EXTRA_MIME_TYPES -> {
                val tmp = it.value as ArrayList<*>
                intent.putExtra(it.key, tmp.toArray(arrayOfNulls<String>(tmp.size)))
              }
              Intent.EXTRA_CONTENT_ANNOTATIONS -> intent.putExtra(
                it.key,
                (it.value as? ArrayList<*>)?.filterIsInstance<String>() as ArrayList<String>
              )
              Intent.EXTRA_ORIGINATING_URI -> intent.putExtra(it.key, it.value as Uri)
              Intent.EXTRA_PROCESS_TEXT, Intent.EXTRA_TEXT, Intent.EXTRA_TITLE -> {
                if (listOf(Intent.ACTION_WEB_SEARCH, Intent.ACTION_SEARCH).contains(
                    intent.action!!
                  )
                )
                  intent.putExtra("query", it.value as CharSequence)
                else
                  intent.putExtra(it.key, it.value as CharSequence)
              }
              // here in this block, we'll try to leverage type information
              // field passed via platform channel
              else -> {
                // if type information for this extra key is
                // provided by developer, then use that type information
                if (typeInfo?.containsKey(it.key)!!) {

                  when (typeInfo[it.key]) {
                    // casting into singular types

                    "boolean" -> intent.putExtra(it.key, it.value as Boolean)
                    "byte" -> intent.putExtra(it.key, it.value as Byte)
                    "short" -> intent.putExtra(it.key, it.value as Short)
                    "int" -> intent.putExtra(it.key, it.value as Int)
                    "long" -> intent.putExtra(it.key, it.value as Long)
                    "float" -> intent.putExtra(it.key, it.value as Float)
                    "double" -> intent.putExtra(it.key, it.value as Double)
                    "char" -> intent.putExtra(it.key, it.value as Char)
                    "String" -> intent.putExtra(it.key, it.value as String)
                    "boolean[]" -> intent.putExtra(
                      it.key,
                      it.value as BooleanArray
                    )
                    "byte[]" -> intent.putExtra(it.key, it.value as ByteArray)
                    "short[]" -> intent.putExtra(it.key, it.value as ShortArray)
                    "int[]" -> intent.putExtra(it.key, it.value as IntArray)
                    "long[]" -> intent.putExtra(it.key, it.value as LongArray)
                    "float[]" -> intent.putExtra(it.key, it.value as FloatArray)
                    "double[]" -> intent.putExtra(
                      it.key,
                      it.value as DoubleArray
                    )
                    "char[]" -> intent.putExtra(it.key, it.value as CharArray)
                    "String[]" -> {
                      val tmp = it.value as ArrayList<*>
                      intent.putExtra(
                        it.key,
                        tmp.toArray(arrayOfNulls<String>(tmp.size))
                      )
                    }
                    // if some unsupported type information supplied by user
                    else -> intent.putExtra(it.key, it.value as String)
                  }

                } else {
                  intent.putExtra(it.key, it.value as String)
                }
              }
            }
          }
        }
        call.argument<List<Int>>("flag")?.forEach {
          intent.addFlags(it)
        }
        call.argument<List<String>>("category")?.forEach {
          intent.addCategory(it)
        }
        if (call.argument<String>("type") != null)
          intent.type = call.argument<String>("type")
        try {
          if (call.argument<Boolean>("chooser")!!) activityAware?.activity?.startActivity(
            Intent.createChooser(intent, "Sharing")
          )
          else activityAware?.activity?.startActivity(intent)
        } catch (e: Exception) {
          result.error("Error", e.toString(), null)
        }
      }
      "startActivityForResult" -> {
        activityCompletedCallBack = object : ActivityCompletedCallBack {
          override fun sendDocument(data: List<String>) {
            result.success(data)
          }
        }
        val activityImageVideoCaptureCode = 998
        val activityIdentifierCode = 999
        val intent = Intent()
        intent.action = call.argument<String>("action")
        if (call.argument<String>("package") != null)
          intent.`package` = call.argument<String>("package")
        if (call.argument<String>("data") != null)
          intent.data = Uri.parse(call.argument<String>("data"))

        // typeInfo parsed into associative array, which can be used for type casting extra data
        val typeInfo = call.argument<Map<String, String>>("typeInfo")

        call.argument<Map<String, Any>>("extra")?.apply {
          this.entries.forEach {
            when (it.key) {
              Intent.EXTRA_DONT_KILL_APP,
              Intent.EXTRA_LOCAL_ONLY,
              Intent.EXTRA_ALLOW_MULTIPLE, Intent.EXTRA_PROCESS_TEXT_READONLY -> intent.putExtra(
                it.key,
                it.value as Boolean
              )
              Intent.EXTRA_EMAIL,
              Intent.EXTRA_BCC,
              Intent.EXTRA_CC, Intent.EXTRA_MIME_TYPES -> {
                val tmp = it.value as ArrayList<*>
                intent.putExtra(it.key, tmp.toArray(arrayOfNulls<String>(tmp.size)))
              }
              Intent.EXTRA_CONTENT_ANNOTATIONS -> intent.putExtra(
                it.key,
                (it.value as? ArrayList<*>)?.filterIsInstance<String>() as ArrayList<String>
              )
              Intent.EXTRA_ORIGINATING_URI -> intent.putExtra(it.key, it.value as Uri)
              Intent.EXTRA_PROCESS_TEXT, Intent.EXTRA_TEXT, Intent.EXTRA_TITLE -> {
                if (listOf(Intent.ACTION_WEB_SEARCH, Intent.ACTION_SEARCH).contains(
                    intent.action!!
                  )
                )
                  intent.putExtra("query", it.value as CharSequence)
                else
                  intent.putExtra(it.key, it.value as CharSequence)
              }
              // here in this block, we'll try to leverage type information
              // field passed via platform channel
              else -> {
                // if type information for this extra key is
                // provided by developer, then use that type information
                if (typeInfo?.containsKey(it.key)!!) {

                  when (typeInfo[it.key]) {
                    // casting into singular types

                    "boolean" -> intent.putExtra(it.key, it.value as Boolean)
                    "byte" -> intent.putExtra(it.key, it.value as Byte)
                    "short" -> intent.putExtra(it.key, it.value as Short)
                    "int" -> intent.putExtra(it.key, it.value as Int)
                    "long" -> intent.putExtra(it.key, it.value as Long)
                    "float" -> intent.putExtra(it.key, it.value as Float)
                    "double" -> intent.putExtra(it.key, it.value as Double)
                    "char" -> intent.putExtra(it.key, it.value as Char)
                    "String" -> intent.putExtra(it.key, it.value as String)

                    // casting into plural types

                    "boolean[]" -> intent.putExtra(
                      it.key,
                      it.value as BooleanArray
                    )
                    "byte[]" -> intent.putExtra(it.key, it.value as ByteArray)
                    "short[]" -> intent.putExtra(it.key, it.value as ShortArray)
                    "int[]" -> intent.putExtra(it.key, it.value as IntArray)
                    "long[]" -> intent.putExtra(it.key, it.value as LongArray)
                    "float[]" -> intent.putExtra(it.key, it.value as FloatArray)
                    "double[]" -> intent.putExtra(
                      it.key,
                      it.value as DoubleArray
                    )
                    "char[]" -> intent.putExtra(it.key, it.value as CharArray)
                    "String[]" -> {
                      val tmp = it.value as ArrayList<*>
                      intent.putExtra(
                        it.key,
                        tmp.toArray(arrayOfNulls<String>(tmp.size))
                      )
                    }
                    // if some unsupported type information supplied by user
                    else -> intent.putExtra(it.key, it.value as String)
                  }

                } else {
                  intent.putExtra(it.key, it.value as String)
                }
              }
            }
          }
        }
        call.argument<List<Int>>("flag")?.forEach {
          intent.addFlags(it)
        }
        call.argument<List<String>>("category")?.forEach {
          intent.addCategory(it)
        }
        if (call.argument<String>("type") != null)
          intent.type = call.argument<String>("type")
        try {
          val activity = activityAware?.activity ?: return
          if (intent.action == MediaStore.ACTION_IMAGE_CAPTURE) {
            intent.resolveActivity(activity.packageManager).also {
              getImageTempFile()?.also {
                tobeCapturedImageLocationFilePath = it
                activityAware?.activity?.packageName
                toBeCapturedImageLocationURI = FileProvider.getUriForFile(
                  activity.applicationContext,
                  "${activity.packageName}.com.fintent.xintent.flutterx_intent.fileProvider",
                  it
                )
                intent.putExtra(
                  MediaStore.EXTRA_OUTPUT,
                  toBeCapturedImageLocationURI
                )
                activity.startActivityForResult(
                  intent,
                  activityImageVideoCaptureCode
                )
              }
            }
          } else if (intent.action == MediaStore.ACTION_VIDEO_CAPTURE) {
            intent.resolveActivity(activity.packageManager).also {
              getVideoTempFile()?.also {
                tobeCapturedImageLocationFilePath = it
                toBeCapturedImageLocationURI = FileProvider.getUriForFile(
                  activity.applicationContext,
                  "${activity.packageName}.com.fintent.xintent.flutterx_intent.fileProvider",
                  it
                )
                intent.putExtra(
                  MediaStore.EXTRA_OUTPUT,
                  toBeCapturedImageLocationURI
                )
                activity.startActivityForResult(
                  intent,
                  activityImageVideoCaptureCode
                )
              }
            }
          } else {
            if (call.argument<Boolean>("chooser")!!) activity.startActivityForResult(
              Intent.createChooser(intent, "Sharing"),
              activityIdentifierCode
            )
            else activity.startActivityForResult(intent, activityIdentifierCode)
          }
        } catch (e: Exception) {
          result.error("Error", e.toString(), null)
        }
      }
      else -> result.notImplemented()
    }
  }

  private fun getImageTempFile(): File? {
    return try {
      val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
      val storageDir =
        activityAware?.activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
      File.createTempFile("IMG_${timeStamp}", ".jpg", storageDir)
    } catch (e: java.lang.Exception) {
      null
    }
  }

  private fun getVideoTempFile(): File? {
    return try {
      val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
      val storageDir =
        activityAware?.activity?.getExternalFilesDir(Environment.DIRECTORY_DCIM)
      File.createTempFile("VIDEO_${timeStamp}", ".mp4", storageDir)
    } catch (e: java.lang.Exception) {
      null
    }
  }

  private fun resolveContacts(uri: Uri): String {
    lateinit var contact: String
    activityAware?.activity?.applicationContext?.contentResolver?.query(
      uri,
      arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
      null,
      null,
      null
    ).apply {
      this?.moveToFirst()
      contact =
        this?.getString(getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))!!
      close()
    }
    return contact
  }

  private fun uriToFilePath(uri: Uri): String {
    val cursor = activityAware?.activity?.applicationContext?.contentResolver?.query(
      uri,
      arrayOf(MediaStore.MediaColumns.DATA),
      null, null, null
    )
    cursor?.moveToFirst()
    val tmp = cursor?.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
    cursor?.close()
    return tmp!!
  }

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(binding.binaryMessenger, "intent")
    channel?.setMethodCallHandler(this)
  }

  override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {
    channel?.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityAware = binding
  }

  override fun onDetachedFromActivity() {
    activityAware = null
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }
}

interface ActivityCompletedCallBack {
  fun sendDocument(data: List<String>)
}