package me.rerere.rikkahub.ui.pages.extensions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState

private const val WHALE_HTML = """
<!doctype html>
<html lang='zh-CN'>
<head>
<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>
<style>
:root { color-scheme: dark; }
* { box-sizing: border-box; }
body { margin:0; font-family: system-ui,sans-serif; background:#101114; color:#f5f5f5; }
main { min-height:100vh; padding:18px; display:flex; flex-direction:column; gap:14px; }
.card { background:#1b1d22; border:1px solid #30333b; border-radius:20px; padding:16px; }
.hero { display:flex; align-items:center; gap:14px; }
.whale { width:74px; height:74px; border-radius:24px; display:grid; place-items:center; font-size:46px; background:#252a35; }
.title { font-size:20px; font-weight:700; }
.sub { color:#aeb3bf; font-size:13px; margin-top:4px; }
.row { display:flex; justify-content:space-between; align-items:center; padding:9px 0; }
.label { color:#aeb3bf; }
.value { font-weight:700; font-variant-numeric:tabular-nums; }
input { width:100%; padding:13px; border-radius:14px; border:1px solid #3a3e48; background:#121419; color:#fff; outline:none; }
button { width:100%; padding:13px; border:0; border-radius:14px; background:#6ea8fe; color:#07101e; font-weight:700; font-size:15px; }
button.secondary { background:#292d36; color:#fff; }
.small { font-size:12px; color:#8f95a3; line-height:1.5; }
.ok { color:#78d89a; } .err { color:#ff8d8d; }
</style>
</head>
<body>
<main>
<div class='card hero'>
  <div class='whale'>🐋</div>
  <div><div class='title'>小鲸鱼记账</div><div class='sub'>RikkaHub 移动版 · DeepSeek</div></div>
</div>
<div class='card'>
  <div class='row'><span class='label'>账户余额</span><span class='value' id='balance'>—</span></div>
  <div class='row'><span class='label'>今日已用</span><span class='value' id='today'>—</span></div>
  <div class='row'><span class='label'>上次刷新</span><span class='value' id='time'>—</span></div>
</div>
<div class='card'>
  <div style='font-weight:700;margin-bottom:10px'>DeepSeek API Key</div>
  <input id='key' type='password' placeholder='sk-...' autocomplete='off'>
  <div style='height:10px'></div>
  <button onclick='saveAndRefresh()'>保存并刷新</button>
  <div style='height:8px'></div>
  <button class='secondary' onclick='refresh()'>只刷新余额</button>
  <div id='status' class='small' style='margin-top:10px'>密钥仅保存在本机 WebView 的本地存储中。</div>
</div>
<div class='small'>这是 RikkaHub 内置的移动版适配实现。余额来自 DeepSeek 官方余额接口；今日已用按本机观察到的余额下降额累计。</div>
</main>
<script>
const K='rikkahub_whale_key', B='rikkahub_whale_balance', D='rikkahub_whale_day', U='rikkahub_whale_today';
const keyEl=document.getElementById('key');
keyEl.value=localStorage.getItem(K)||'';
function money(v){return typeof v==='number'?('¥'+v.toFixed(4)):'—'}
function status(t,err=false){const e=document.getElementById('status');e.textContent=t;e.className='small '+(err?'err':'ok')}
function load(){
 const day=new Date().toISOString().slice(0,10);
 if(localStorage.getItem(D)!==day){localStorage.setItem(D,day);localStorage.setItem(U,'0');}
 const b=Number(localStorage.getItem(B));
 document.getElementById('balance').textContent=Number.isFinite(b)&&b>0?money(b):'—';
 document.getElementById('today').textContent=money(Number(localStorage.getItem(U)||0));
}
async function refresh(){
 const key=localStorage.getItem(K)||keyEl.value.trim();
 if(!key){status('请先填写 API Key',true);return;}
 status('正在刷新…');
 try{
   const r=await fetch('https://api.deepseek.com/user/balance',{headers:{'Authorization':'Bearer '+key,'Accept':'application/json'}});
   if(!r.ok) throw new Error('HTTP '+r.status);
   const j=await r.json();
   const list=j.balance_infos||[];
   const c=list.find(x=>x.currency==='CNY')||list[0];
   const now=Number(c?.total_balance);
   if(!Number.isFinite(now)) throw new Error('接口未返回余额');
   const old=Number(localStorage.getItem(B));
   if(Number.isFinite(old)&&now<old){localStorage.setItem(U,String(Number(localStorage.getItem(U)||0)+(old-now)));}
   localStorage.setItem(B,String(now));
   localStorage.setItem(D,new Date().toISOString().slice(0,10));
   document.getElementById('time').textContent=new Date().toLocaleTimeString();
   load(); status('刷新成功');
 }catch(e){status('刷新失败：'+e.message,true);load();}
}
function saveAndRefresh(){localStorage.setItem(K,keyEl.value.trim());refresh();}
load();
</script>
</body>
</html>
"""

@Composable
fun WhaleWidgetPage() {
    val state = rememberWebViewState(
        data = WHALE_HTML,
        baseUrl = "https://rikkahub.local/",
        mimeType = "text/html",
        settings = {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        },
    )

    BackHandler(enabled = state.canGoBack) {
        state.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小鲸鱼记账") },
                navigationIcon = { BackButton() },
            )
        },
    ) { padding ->
        WebView(
            state = state,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
