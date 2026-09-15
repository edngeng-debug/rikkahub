(function(){
  var s=document.createElement('style');
  s.textContent='.dshwv-debug-mask,.dshwv-debug-panel,.dshwv-secondary-mask,.dshwv-secondary-overlay{display:none!important}';
  document.head.appendChild(s);
  var hide=function(){document.querySelectorAll('[class*="debug-mask"],[class*="secondary-mask"]').forEach(function(e){e.style.display='none'})};
  hide();
  new MutationObserver(hide).observe(document.body,{childList:true,subtree:true});
})();