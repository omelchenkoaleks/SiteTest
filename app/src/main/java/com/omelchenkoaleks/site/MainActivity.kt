package com.omelchenkoaleks.site

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var mWebView: WebView
    lateinit var mDrawerLayout: DrawerLayout
    lateinit var mNavigationView: NavigationView
    // хранит состояние доступа к Интернету
    var networkAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        var url = getString(R.string.website_main)
        // TODO: пока здесь ссылка на поиск тура - должна быть форма обратной связи
        var urlFeedback = getString(R.string.website_tour_search)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mNavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        mNavigationView.setNavigationItemSelectedListener(this)

        mWebView = findViewById(R.id.web_view)
        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.setAppCacheEnabled(false)

        loadWebSite(mWebView, url, applicationContext)

        // разноцветные вращающиеся круги прогрессбара
        swipe_refresh_layout.setColorSchemeResources(R.color.colorRed, R.color.colorBlue, R.color.colorGreen)
        swipe_refresh_layout.apply {
            setOnRefreshListener {
                if (mWebView.url != null) url = mWebView.url
                loadWebSite(mWebView, url, applicationContext)
            }

            setOnChildScrollUpCallback { parent, child -> mWebView.scrollY > 0 }
        }

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            loadWebSite(mWebView, urlFeedback, applicationContext)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val url = getString(R.string.website_main)
                loadWebSite(mWebView, url, applicationContext)
            }
            R.id.nav_tour_search -> {
                val url = getString(R.string.website_tour_search)
                loadWebSite(mWebView, url, applicationContext)
            }
            R.id.nav_countries -> {
                val url = getString(R.string.website_countries)
                loadWebSite(mWebView, url, applicationContext)
            }
            R.id.nav_types_of_rest -> {
                val url = getString(R.string.website_types_of_rest)
                loadWebSite(mWebView, url, applicationContext)
            }
            R.id.nav_about_us -> {
                val url = getString(R.string.website_about_us)
                loadWebSite(mWebView, url, applicationContext)
            }
        }
        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    // загружаем сайт
    private fun loadWebSite(mWebView: WebView, url: String, context: Context) {
        progress_bar.visibility = View.VISIBLE
        networkAvailable = isNetworkAvailable(context)

        // очищаем кэш, чтобы избежать некоторых ошибок
        mWebView.clearCache(true)

        if (networkAvailable) {
            webViewVisible(mWebView)
            mWebView.webViewClient = MyWebViewClient()
            mWebView.loadUrl(url)
        } else {
            webViewGone(mWebView)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun webViewVisible(mWebView: WebView) {
        mWebView.visibility = View.VISIBLE
        check_connection_text_view.visibility = View.GONE
    }

    private fun webViewGone(mWebView: WebView) {
        mWebView.visibility = View.GONE
        check_connection_text_view.visibility = View.VISIBLE
        progress_bar.visibility = View.GONE
    }

    // проверяем доступ к сети
    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(context: Context) : Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return  if (Build.VERSION.SDK_INT > 22) {
                val an = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(an) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                val a = cm.activeNetworkInfo ?: return false
                a.isConnected && (a.type == ConnectivityManager.TYPE_WIFI || a.type == ConnectivityManager.TYPE_MOBILE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun onLoadComplete() {
        swipe_refresh_layout.isRefreshing = false
        progress_bar.visibility = View.GONE
    }

    private inner class MyWebViewClient : WebViewClient() {

        @RequiresApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            return urlOverride(url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            return urlOverride(url)
        }

        private fun urlOverride(url: String) : Boolean {
            progress_bar.visibility = View.VISIBLE
            networkAvailable = isNetworkAvailable(applicationContext)

            if (networkAvailable) {
                if (Uri.parse(url).host == getString(R.string.website_main)) return false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                onLoadComplete()
                return true
            } else {
                webViewGone(web_view)
                return false
            }
        }

        @Suppress("DEPRECATION")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            // код ошибки равен 0 при отсутствии сети
            if (errorCode == 0) {
                view?.visibility = View.GONE
                check_connection_text_view.visibility = View.VISIBLE
                onLoadComplete()
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            onReceivedError(view, error!!.errorCode, error.description.toString(), request!!.url.toString())
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onLoadComplete()
        }

    }

}
