package me.rerere.rikkahub.ui.pages.extensions

import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import me.rerere.rikkahub.whale.DaFeiYuSettingsStore
import org.json.JSONObject

@Composable
fun DaFeiYuAdvancedEditorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val loading = remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportZoom(false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                                return interceptWhaleRequest(context, request)
                            }
                        }

                        val whale = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
                        val html = """
                            <!doctype html>
                            <html><head>
                            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                            <style>
                              html,body{margin:0;width:100%;height:100%;background:transparent;overflow:auto}
                              .dshwv-root{display:none!important}
                              .dshwv-menu{position:relative!important;left:auto!important;right:auto!important;top:auto!important;bottom:auto!important;
                                opacity:1!important;transform:none!important;pointer-events:auto!important;display:block!important;
                                margin:16px auto!important;max-width:680px;box-sizing:border-box}
                              .dshwv-menu-btn{display:none!important}
                            </style></head>
                            <body>
                            <script>
                              window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;
                              window.__RIKKAHUB_DAFEIYU_SETTINGS=true;
                              (function(){
                                const nativeFetch=window.fetch.bind(window);
                                const prefix='dafeiyu-editor:';
                                const core=new Set(['balance.json','api-models.json','size.json','usage-settings.json','last-turn.json']);
                                function key(path){return prefix+path;}
                                function response(value,status){return new Response(JSON.stringify(value),{status:status,headers:{'Content-Type':'application/json'}});}
                                window.fetch=async function(input,init){
                                  try{
                                    const u=new URL(typeof input==='string'?input:input.url,location.href);
                                    if(!u.pathname.startsWith('/dsh-whale/') || core.has(u.pathname.split('/').pop())) return nativeFetch(input,init);
                                    const path=u.pathname;
                                    const method=String((init&&init.method)||'GET').toUpperCase();
                                    if(method==='GET'){
                                      const raw=localStorage.getItem(key(path));
                                      if(raw) return response(JSON.parse(raw),200);
                                      if(path.endsWith('/audio.json')) return response({ok:true,groups:[],fragments:[{id:'exp_orb',name:'Minecraft·经验球'},{id:'end_a',name:'A'}]},200);
                                      if(path.endsWith('/roles.json')) return response({ok:true,roles:[{id:'default',name:'大肥鱼'}]},200);
                                      if(path.endsWith('/bubble.json')) return response({ok:true,config:{},library:[]},200);
                                      if(path.endsWith('/api.json')) return response({ok:true,models:[],templates:[]},200);
                                      return response({ok:true},200);
                                    }
                                    let value={ok:true};
                                    if(init&&init.body&&typeof init.body==='string'){
                                      try{value=JSON.parse(init.body)}catch(e){}
                                    }
                                    localStorage.setItem(key(path),JSON.stringify(value));
                                    return response({ok:true},200);
                                  }catch(e){ return nativeFetch(input,init); }
                                };
                              })();
                            </script>
                            <script>$whale</script>
                            <script>
                              setTimeout(function(){
                                try{
                                  var b=document.querySelector('.dshwv-menu');
                                  if(b){b.style.display='block';b.classList.add('dshwv-menu-open');}
                                }catch(e){}
                              },120);
                            </script>
                            </body></html>
                        """.replace("$whale", whale)
                        loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)
                        postDelayed({ loading.value = false }, 350)
                    }
                },
            )
            if (loading.value) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun interceptWhaleRequest(context: android.content.Context, request: WebResourceRequest): WebResourceResponse? {
    return when (request.url.path) {
        "/dsh-whale/image.png" -> asset(context, "dafeiyu/DSniang1.png", "image/png")
        "/dsh-whale/rua.gif" -> asset(context, "dafeiyu/rua.gif", "image/gif")
        "/dsh-whale/sound/press.mp3" -> asset(context, if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3", "audio/mpeg")
        "/dsh-whale/sound/release.mp3" -> asset(context, if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3", "audio/mpeg")
        "/dsh-whale/audio-fragment.wav" -> {
            val id = request.url.getQueryParameter("id")
            val path = if (id == "end_a") "dafeiyu/task-end-a.wav" else "dafeiyu/minecraft-exp-orb.wav"
            asset(context, path, "audio/wav")
        }
        "/dsh-whale/bubble-petpet.gif" -> asset(context, "dafeiyu/bubble-petpet.gif", "image/gif")
        "/dsh-whale/bubble-money1.gif" -> asset(context, "dafeiyu/bubble-money1.gif", "image/gif")
        else -> null
    }
}

private fun asset(context: android.content.Context, path: String, mime: String): WebResourceResponse =
    WebResourceResponse(mime, null, context.assets.open(path))
