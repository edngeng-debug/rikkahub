/* RikkaHub embedded host: full-screen transparent WebView, native whale drag stays in JS. */
(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;
  function sync(){
    try{
      var menu=document.querySelector('.dshwv-menu');
      if(menu)menu.style.setProperty('display','none','important');
      var buttons=document.querySelectorAll('.dshwv-menu-btn,.dshwv-menu-btn-visible');
      for(var i=0;i<buttons.length;i++)buttons[i].style.setProperty('display','none','important');
    }catch(e){}
  }
  setTimeout(sync,120);
  setInterval(sync,1000);
})();
