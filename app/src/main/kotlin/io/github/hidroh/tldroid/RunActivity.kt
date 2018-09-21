package io.github.hidroh.tldroid

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.util.Pair
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView
import java.io.IOException
import java.lang.ref.WeakReference

class RunActivity : ThemedActivity() {
  companion object {
    val EXTRA_COMMAND = RunActivity::class.java.name + ".EXTRA_COMMAND"
    private const val STATE_ERROR = "state:error"
    private const val STATE_OUTPUT = "state:output"
  }

  private var mOutput: TextView? = null
  private var mError: TextView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DataBindingUtil.setContentView<ViewDataBinding>(this, R.layout.activity_run)
    val snackbar = Snackbar.make(findViewById(android.R.id.content),
        R.string.run_warning, Snackbar.LENGTH_INDEFINITE)
    snackbar.setAction(android.R.string.ok, { snackbar.dismiss() }).show()
    mOutput = findViewById(R.id.output)
    mError = findViewById(R.id.error)
    val command = intent.getStringExtra(EXTRA_COMMAND)
    (findViewById<TextView>(R.id.prompt)).append(command)
    (findViewById<EditText>(R.id.edit_text)).setOnEditorActionListener { v, _, _ ->
      execute(command, v.text.toString().trim())
      true
    }
    if (savedInstanceState != null) {
      display(Pair.create(savedInstanceState.getString(STATE_OUTPUT),
          savedInstanceState.getString(STATE_ERROR)))
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_ERROR, mError!!.text.toString())
    outState.putString(STATE_OUTPUT, mOutput!!.text.toString())
  }

  private fun execute(command: String, arguments: String) {
    if (TextUtils.isEmpty(arguments)) {
      RunTask(this).execute(command)
    } else {
      RunTask(this).execute(*TextUtils.split("$command $arguments", "\\s+"))
    }
  }

  private fun display(output: Pair<String, String>) {
    if (!TextUtils.isEmpty(output.second)) {
      mError!!.text = output.second
      mError!!.visibility = View.VISIBLE
      mOutput!!.visibility = View.GONE
    } else {
      mOutput!!.text = output.first
      mOutput!!.visibility = View.VISIBLE
      mError!!.visibility = View.GONE
    }
  }

  internal class RunTask(runActivity: RunActivity) :
      AsyncTask<String, Void, Pair<String, String>>() {
    private val mRunActivity = WeakReference(runActivity)

    override fun doInBackground(vararg params: String): Pair<String, String> {
      return try {
          val process = Runtime.getRuntime().exec(params)
          val stderr = Utils.readUtf8(process.errorStream)
          val stdout = Utils.readUtf8(process.inputStream)
          process.destroy()
        Pair.create(stdout, stderr)
      } catch (e: IOException) {
        Pair.create<String, String>(null, e.message)
      }

    }

    override fun onPostExecute(output: Pair<String, String>) {
      if (mRunActivity.get() != null) {
        (mRunActivity.get() as RunActivity).display(output)
      }
    }
  }
}
