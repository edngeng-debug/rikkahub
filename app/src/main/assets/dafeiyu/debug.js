/* 大肥鱼嵌入版：聊天页只负责显示/交互；完整设置统一放到「插件 > 大肥鱼」。 */
(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;
  var timer=0,lastInteractive=null,lastRect=null;

  function setInteractive(v){
    if(!window.RikkaDaFeiYu||lastInteractive===v)return;
    lastInteractive=v;
    try{window.RikkaDaFeiYu.setInteractive(v)}catch(e){}
  }

  function reportRect(){
    try{
      var r=document.querySelector('.dshwv-root');
      if(!r||!window.RikkaDaFeiYu)return;
      var b=r.getBoundingClientRect(),next=[b.left,b.top,b.right,b.bottom,window.innerWidth,window.innerHeight];
      if(lastRect&&next.every(function(v,i){return Math.abs(v-lastRect[i])<.5}))return;
      lastRect=next;
      window.RikkaDaFeiYu.setWidgetRect.apply(window.RikkaDaFeiYu,next);
    }catch(e){}
  }

  function sync(){
    try{
      var open=!!document.querySelector('.dshwv-menu.dshwv-menu-open,.dshwv-root.dshwv-dragging');
      if(!open){
        var n=document.querySelectorAll('.dshwv-menu,.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask,.dshwv-qedit,.dshwv-usagepanel,.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu,.dshwv-hintbox');
        for(var i=0;i<n.length;i++){
          var e=n[i],c=getComputedStyle(e),r=e.getBoundingClientRect();
          if(c.display!=='none'&&c.visibility!=='hidden'&&r.width>1&&r.height>1){open=true;break}
        }
      }
      setInteractive(open);
      reportRect();
    }catch(e){}
  }

  function schedule(){if(timer)return;timer=setTimeout(function(){timer=0;sync()},60)}

  function install(){
    if(document.querySelector('.dshwv-embedded-settings-style'))return;
    var st=document.createElement('style');
    st.className='dshwv-embedded-settings-style';
    st.textContent=[
      'html,body,#root{width:100%!important;height:100%!important;margin:0!important;overflow:visible!important}',
      '.dshwv-root{--dshw-base:250px!important;width:250px!important;height:250px!important;right:0!important;bottom:0!important}',
      '.dshwv-root.dshwv-left{transform:none!important}',
      '.dshwv-root.dshwv-left .dshwv-img{transform:scaleX(-1)!important;transform-origin:right bottom!important}',
      '.dshwv-root.dshwv-left .dshwv-gif{transform:translate(-50%,-50%) scaleX(-1)!important}',
      '.dshwv-root.dshwv-left .dshwv-text{transform:translate(-50%,-50%)!important}',
      '.dshwv-menu{position:fixed!important;left:auto!important;right:12px!important;top:12px!important;bottom:auto!important;width:min(400px,calc(100vw - 24px))!important;min-width:min(280px,calc(100vw - 24px))!important;max-width:calc(100vw - 24px)!important;max-height:calc(100vh - 24px)!important;overflow-y:auto!important;overflow-x:hidden!important;box-sizing:border-box!important;z-index:100000!important}',
      '.dshwv-menu>.dshwv-menu-row:first-child{display:none!important}',
      '.dshwv-menu-row{flex-wrap:wrap!important;min-width:0!important}',
      '.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask{position:fixed!important;left:0!important;top:0!important;right:0!important;bottom:0!important;width:100vw!important;height:100vh!important;min-width:100vw!important;min-height:100vh!important;max-width:none!important;max-height:none!important;box-sizing:border-box!important;align-items:center!important;justify-content:center!important;overflow:auto!important;z-index:100001!important}',
      '.dshwv-bubcard{width:min(440px,calc(100vw - 24px))!important;max-width:calc(100vw - 24px)!important;max-height:88vh!important;overflow-y:auto!important;overflow-x:hidden!important;box-sizing:border-box!important;flex:0 0 auto!important}',
      '.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu,.dshwv-hintbox,.dshwv-qedit,.dshwv-usagepanel{z-index:100002!important;max-width:calc(100vw - 24px)!important}'
    ].join('');
    document.head.appendChild(st);
  }

  function wait(){
    var r=document.querySelector('.dshwv-root');
    if(!r){setTimeout(wait,120);return}
    install();
    sync();
    try{new MutationObserver(schedule).observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style','hidden']})}catch(e){}
    setInterval(sync,500);
  }
  wait();
})();
