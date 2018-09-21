package io.github.hidroh.tldroid

import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.text.HtmlCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.ActionBar
import android.support.v7.widget.ShareActionProvider
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.lang.ref.WeakReference

class CommandActivity : ThemedActivity() {
  companion object {
    val EXTRA_QUERY = CommandActivity::class.java.name + ".EXTRA_QUERY"
    val EXTRA_PLATFORM = CommandActivity::class.java.name + ".EXTRA_PLATFORM"
    private const val STATE_CONTENT = "state:content"
    private const val PLATFORM_OSX = "osx"
  }

  private var mContent: String? = null
  private var mQuery: String? = null
  private var mPlatform: String? = null
  private var mBinding: ViewDataBinding? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mQuery = intent.getStringExtra(EXTRA_QUERY)
    mPlatform = intent.getStringExtra(EXTRA_PLATFORM)
    title = mQuery
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_command)
    setSupportActionBar(findViewById(R.id.toolbar))
    supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_HOME or
        ActionBar.DISPLAY_HOME_AS_UP or ActionBar.DISPLAY_SHOW_TITLE
    val collapsingToolbar = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_layout)
    collapsingToolbar!!.setExpandedTitleTypeface(Application.MONOSPACE_TYPEFACE)
    collapsingToolbar.setCollapsedTitleTypeface(Application.MONOSPACE_TYPEFACE)
    val webView = findViewById<WebView>(R.id.web_view)
    webView!!.webChromeClient = WebChromeClient()
    webView.webViewClient = object : WebViewClient() {
      @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
      override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        startActivity(Intent(Intent.ACTION_VIEW, request.url))
        return true
      }

      override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return true
      }
    }
    if (savedInstanceState != null) {
      mContent = savedInstanceState.getString(STATE_CONTENT)
    }
    if (mContent == null) {
      GetCommandTask(this, mPlatform).execute(mQuery)
    } else {
      render(mContent)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_run, menu)
    menuInflater.inflate(R.menu.menu_share, menu)
    // disable share history
    MenuItemCompat.setActionProvider(menu.findItem(R.id.menu_share),
        object : ShareActionProvider(this) {
          override fun onCreateActionView(): View? {
            return null
          }
        })
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    val itemShare = menu.findItem(R.id.menu_share)
    val visible = !TextUtils.isEmpty(mContent)
    itemShare.isVisible = visible
    if (visible) {
      (MenuItemCompat.getActionProvider(itemShare) as ShareActionProvider)
          .setShareIntent(Intent(Intent.ACTION_SEND)
              .setType("text/plain")
              .putExtra(Intent.EXTRA_SUBJECT, mQuery)
              .putExtra(Intent.EXTRA_TEXT, HtmlCompat.fromHtml(mContent!!, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()))
    }
    menu.findItem(R.id.menu_run).isVisible = visible && !TextUtils.equals(PLATFORM_OSX, mPlatform)
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    if (item.itemId == R.id.menu_run) {
      startActivity(Intent(this, RunActivity::class.java)
          .putExtra(RunActivity.EXTRA_COMMAND, mQuery))
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_CONTENT, mContent)
  }

  internal fun render(html: String?) {
    mContent = html ?: ""
    invalidateOptionsMenu()
    // just display a generic message if empty for now
    mBinding!!.setVariable(io.github.hidroh.tldroid.BR.content,
        if (TextUtils.isEmpty(mContent)) getString(R.string.empty_html) else html)
  }

  internal class GetCommandTask(commandActivity: CommandActivity, platform: String?) :
      AsyncTask<String, Void, String>() {

    private val commandActivity: WeakReference<CommandActivity> = WeakReference(commandActivity)
    private val processor: MarkdownProcessor = MarkdownProcessor(platform)

    override fun doInBackground(vararg params: String): String? {
      val context = commandActivity.get() ?: return null
      val commandName = params[0]
      val lastModified = PreferenceManager.getDefaultSharedPreferences(context)
          .getLong(SyncService.PREF_LAST_ZIPPED, 0L)
      return processor.process(context, commandName, lastModified)
    }

    override fun onPostExecute(s: String?) {
      if (commandActivity.get() != null) {
        (commandActivity.get() as CommandActivity).render(s)
      }
    }
  }
}
