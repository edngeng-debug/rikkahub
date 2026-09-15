/* Keep the checked-in 大肥鱼 debug layer as the build trigger source. */
(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;
  var KEY='dshw-debug-size-v5';
  var defaults={char:1,bubble:1,label:1,amount:1,hint:1,button:1};
  var lastInteractive=null,lastRect=null,observerTimer=0,started=false;
  function stateLoad(){try{return Object.assign({},defaults,JSON.parse(localStorage.getItem(KEY)||'{}'))}catch(e){return Object.assign({},defaults)}}
  function stateSave(s){try{localStorage.setItem(KEY,JSON.stringify(s))}catch(e){}}
