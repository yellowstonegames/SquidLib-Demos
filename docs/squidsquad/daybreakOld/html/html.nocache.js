function html(){
  var $intern_0 = 'bootstrap', $intern_1 = 'begin', $intern_2 = 'gwt.codesvr.html=', $intern_3 = 'gwt.codesvr=', $intern_4 = 'html', $intern_5 = 'startup', $intern_6 = 'DUMMY', $intern_7 = 0, $intern_8 = 1, $intern_9 = 'iframe', $intern_10 = 'position:absolute; width:0; height:0; border:none; left: -1000px;', $intern_11 = ' top: -1000px;', $intern_12 = 'CSS1Compat', $intern_13 = '<!doctype html>', $intern_14 = '', $intern_15 = '<html><head><\/head><body><\/body><\/html>', $intern_16 = 'undefined', $intern_17 = 'readystatechange', $intern_18 = 10, $intern_19 = 'script', $intern_20 = 'javascript', $intern_21 = 'Failed to load ', $intern_22 = 'moduleStartup', $intern_23 = 'scriptTagAdded', $intern_24 = 'moduleRequested', $intern_25 = 'meta', $intern_26 = 'name', $intern_27 = 'html::', $intern_28 = '::', $intern_29 = 'gwt:property', $intern_30 = 'content', $intern_31 = '=', $intern_32 = 'gwt:onPropertyErrorFn', $intern_33 = 'Bad handler "', $intern_34 = '" for "gwt:onPropertyErrorFn"', $intern_35 = 'gwt:onLoadErrorFn', $intern_36 = '" for "gwt:onLoadErrorFn"', $intern_37 = '#', $intern_38 = '?', $intern_39 = '/', $intern_40 = 'img', $intern_41 = 'clear.cache.gif', $intern_42 = 'baseUrl', $intern_43 = 'html.nocache.js', $intern_44 = 'base', $intern_45 = '//', $intern_46 = 'user.agent', $intern_47 = 'webkit', $intern_48 = 'safari', $intern_49 = 'gecko', $intern_50 = 11, $intern_51 = 'gecko1_8', $intern_52 = 'selectingPermutation', $intern_53 = 'html.devmode.js', $intern_54 = 'D63A06513B99097BE248D86E5128767E', $intern_55 = ':1', $intern_56 = ':', $intern_57 = '.cache.js', $intern_58 = 'link', $intern_59 = 'rel', $intern_60 = 'stylesheet', $intern_61 = 'href', $intern_62 = 'head', $intern_63 = 'loadExternalRefs', $intern_64 = 'gwt/chrome/chrome.css', $intern_65 = 'end', $intern_66 = 'http:', $intern_67 = 'file:', $intern_68 = '_gwt_dummy_', $intern_69 = '__gwtDevModeHook:html', $intern_70 = 'Ignoring non-whitelisted Dev Mode URL: ', $intern_71 = ':moduleBase';
  var $wnd = window;
  var $doc = document;
  sendStats($intern_0, $intern_1);
  function isHostedMode(){
    var query = $wnd.location.search;
    return query.indexOf($intern_2) != -1 || query.indexOf($intern_3) != -1;
  }

  function sendStats(evtGroupString, typeString){
    if ($wnd.__gwtStatsEvent) {
      $wnd.__gwtStatsEvent({moduleName:$intern_4, sessionId:$wnd.__gwtStatsSessionId, subSystem:$intern_5, evtGroup:evtGroupString, millis:(new Date).getTime(), type:typeString});
    }
  }

  html.__sendStats = sendStats;
  html.__moduleName = $intern_4;
  html.__errFn = null;
  html.__moduleBase = $intern_6;
  html.__softPermutationId = $intern_7;
  html.__computePropValue = null;
  html.__getPropMap = null;
  html.__installRunAsyncCode = function(){
  }
  ;
  html.__gwtStartLoadingFragment = function(){
    return null;
  }
  ;
  html.__gwt_isKnownPropertyValue = function(){
    return false;
  }
  ;
  html.__gwt_getMetaProperty = function(){
    return null;
  }
  ;
  var __propertyErrorFunction = null;
  var activeModules = $wnd.__gwt_activeModules = $wnd.__gwt_activeModules || {};
  activeModules[$intern_4] = {moduleName:$intern_4};
  html.__moduleStartupDone = function(permProps){
    var oldBindings = activeModules[$intern_4].bindings;
    activeModules[$intern_4].bindings = function(){
      var props = oldBindings?oldBindings():{};
      var embeddedProps = permProps[html.__softPermutationId];
      for (var i = $intern_7; i < embeddedProps.length; i++) {
        var pair = embeddedProps[i];
        props[pair[$intern_7]] = pair[$intern_8];
      }
      return props;
    }
    ;
  }
  ;
  var frameDoc;
  function getInstallLocationDoc(){
    setupInstallLocation();
    return frameDoc;
  }

  function setupInstallLocation(){
    if (frameDoc) {
      return;
    }
    var scriptFrame = $doc.createElement($intern_9);
    scriptFrame.id = $intern_4;
    scriptFrame.style.cssText = $intern_10 + $intern_11;
    scriptFrame.tabIndex = -1;
    $doc.body.appendChild(scriptFrame);
    frameDoc = scriptFrame.contentWindow.document;
    frameDoc.open();
    var doctype = document.compatMode == $intern_12?$intern_13:$intern_14;
    frameDoc.write(doctype + $intern_15);
    frameDoc.close();
  }

  function installScript(filename){
    function setupWaitForBodyLoad(callback){
      function isBodyLoaded(){
        if (typeof $doc.readyState == $intern_16) {
          return typeof $doc.body != $intern_16 && $doc.body != null;
        }
        return /loaded|complete/.test($doc.readyState);
      }

      var bodyDone = isBodyLoaded();
      if (bodyDone) {
        callback();
        return;
      }
      function checkBodyDone(){
        if (!bodyDone) {
          if (!isBodyLoaded()) {
            return;
          }
          bodyDone = true;
          callback();
          if ($doc.removeEventListener) {
            $doc.removeEventListener($intern_17, checkBodyDone, false);
          }
          if (onBodyDoneTimerId) {
            clearInterval(onBodyDoneTimerId);
          }
        }
      }

      if ($doc.addEventListener) {
        $doc.addEventListener($intern_17, checkBodyDone, false);
      }
      var onBodyDoneTimerId = setInterval(function(){
        checkBodyDone();
      }
      , $intern_18);
    }

    function installCode(code_0){
      var doc = getInstallLocationDoc();
      var docbody = doc.body;
      var script = doc.createElement($intern_19);
      script.language = $intern_20;
      script.crossOrigin = $intern_14;
      script.src = code_0;
      if (html.__errFn) {
        script.onerror = function(){
          html.__errFn($intern_4, new Error($intern_21 + code_0));
        }
        ;
      }
      docbody.appendChild(script);
      sendStats($intern_22, $intern_23);
    }

    sendStats($intern_22, $intern_24);
    setupWaitForBodyLoad(function(){
      installCode(filename);
    }
    );
  }

  html.__startLoadingFragment = function(fragmentFile){
    return computeUrlForResource(fragmentFile);
  }
  ;
  html.__installRunAsyncCode = function(code_0){
    var doc = getInstallLocationDoc();
    var docbody = doc.body;
    var script = doc.createElement($intern_19);
    script.language = $intern_20;
    script.text = code_0;
    docbody.appendChild(script);
  }
  ;
  function processMetas(){
    var metaProps = {};
    var propertyErrorFunc;
    var onLoadErrorFunc;
    var metas = $doc.getElementsByTagName($intern_25);
    for (var i = $intern_7, n = metas.length; i < n; ++i) {
      var meta = metas[i], name_0 = meta.getAttribute($intern_26), content_0;
      if (name_0) {
        name_0 = name_0.replace($intern_27, $intern_14);
        if (name_0.indexOf($intern_28) >= $intern_7) {
          continue;
        }
        if (name_0 == $intern_29) {
          content_0 = meta.getAttribute($intern_30);
          if (content_0) {
            var value_0, eq = content_0.indexOf($intern_31);
            if (eq >= $intern_7) {
              name_0 = content_0.substring($intern_7, eq);
              value_0 = content_0.substring(eq + $intern_8);
            }
             else {
              name_0 = content_0;
              value_0 = $intern_14;
            }
            metaProps[name_0] = value_0;
          }
        }
         else if (name_0 == $intern_32) {
          content_0 = meta.getAttribute($intern_30);
          if (content_0) {
            try {
              propertyErrorFunc = eval(content_0);
            }
             catch (e) {
              alert($intern_33 + content_0 + $intern_34);
            }
          }
        }
         else if (name_0 == $intern_35) {
          content_0 = meta.getAttribute($intern_30);
          if (content_0) {
            try {
              onLoadErrorFunc = eval(content_0);
            }
             catch (e) {
              alert($intern_33 + content_0 + $intern_36);
            }
          }
        }
      }
    }
    __gwt_getMetaProperty = function(name_0){
      var value_0 = metaProps[name_0];
      return value_0 == null?null:value_0;
    }
    ;
    __propertyErrorFunction = propertyErrorFunc;
    html.__errFn = onLoadErrorFunc;
  }

  function computeScriptBase(){
    function getDirectoryOfFile(path){
      var hashIndex = path.lastIndexOf($intern_37);
      if (hashIndex == -1) {
        hashIndex = path.length;
      }
      var queryIndex = path.indexOf($intern_38);
      if (queryIndex == -1) {
        queryIndex = path.length;
      }
      var slashIndex = path.lastIndexOf($intern_39, Math.min(queryIndex, hashIndex));
      return slashIndex >= $intern_7?path.substring($intern_7, slashIndex + $intern_8):$intern_14;
    }

    function ensureAbsoluteUrl(url_0){
      if (url_0.match(/^\w+:\/\//)) {
      }
       else {
        var img = $doc.createElement($intern_40);
        img.src = url_0 + $intern_41;
        url_0 = getDirectoryOfFile(img.src);
      }
      return url_0;
    }

    function tryMetaTag(){
      var metaVal = __gwt_getMetaProperty($intern_42);
      if (metaVal != null) {
        return metaVal;
      }
      return $intern_14;
    }

    function tryNocacheJsTag(){
      var scriptTags = $doc.getElementsByTagName($intern_19);
      for (var i = $intern_7; i < scriptTags.length; ++i) {
        if (scriptTags[i].src.indexOf($intern_43) != -1) {
          return getDirectoryOfFile(scriptTags[i].src);
        }
      }
      return $intern_14;
    }

    function tryBaseTag(){
      var baseElements = $doc.getElementsByTagName($intern_44);
      if (baseElements.length > $intern_7) {
        return baseElements[baseElements.length - $intern_8].href;
      }
      return $intern_14;
    }

    function isLocationOk(){
      var loc = $doc.location;
      return loc.href == loc.protocol + $intern_45 + loc.host + loc.pathname + loc.search + loc.hash;
    }

    var tempBase = tryMetaTag();
    if (tempBase == $intern_14) {
      tempBase = tryNocacheJsTag();
    }
    if (tempBase == $intern_14) {
      tempBase = tryBaseTag();
    }
    if (tempBase == $intern_14 && isLocationOk()) {
      tempBase = getDirectoryOfFile($doc.location.href);
    }
    tempBase = ensureAbsoluteUrl(tempBase);
    return tempBase;
  }

  function computeUrlForResource(resource){
    if (resource.match(/^\//)) {
      return resource;
    }
    if (resource.match(/^[a-zA-Z]+:\/\//)) {
      return resource;
    }
    return html.__moduleBase + resource;
  }

  function getCompiledCodeFilename(){
    var answers = [];
    var softPermutationId = $intern_7;
    function unflattenKeylistIntoAnswers(propValArray, value_0){
      var answer = answers;
      for (var i = $intern_7, n = propValArray.length - $intern_8; i < n; ++i) {
        answer = answer[propValArray[i]] || (answer[propValArray[i]] = []);
      }
      answer[propValArray[n]] = value_0;
    }

    var values = [];
    var providers = [];
    function computePropValue(propName){
      var value_0 = providers[propName](), allowedValuesMap = values[propName];
      if (value_0 in allowedValuesMap) {
        return value_0;
      }
      var allowedValuesList = [];
      for (var k in allowedValuesMap) {
        allowedValuesList[allowedValuesMap[k]] = k;
      }
      if (__propertyErrorFunction) {
        __propertyErrorFunction(propName, allowedValuesList, value_0);
      }
      throw null;
    }

    providers[$intern_46] = function(){
      var ua = navigator.userAgent.toLowerCase();
      var docMode = $doc.documentMode;
      if (function(){
        return ua.indexOf($intern_47) != -1;
      }
      ())
        return $intern_48;
      if (function(){
        return ua.indexOf($intern_49) != -1 || docMode >= $intern_50;
      }
      ())
        return $intern_51;
      return $intern_14;
    }
    ;
    values[$intern_46] = {'gecko1_8':$intern_7, 'safari':$intern_8};
    __gwt_isKnownPropertyValue = function(propName, propValue){
      return propValue in values[propName];
    }
    ;
    html.__getPropMap = function(){
      var result = {};
      for (var key in values) {
        if (values.hasOwnProperty(key)) {
          result[key] = computePropValue(key);
        }
      }
      return result;
    }
    ;
    html.__computePropValue = computePropValue;
    $wnd.__gwt_activeModules[$intern_4].bindings = html.__getPropMap;
    sendStats($intern_0, $intern_52);
    if (isHostedMode()) {
      return computeUrlForResource($intern_53);
    }
    var strongName;
    try {
      unflattenKeylistIntoAnswers([$intern_51], $intern_54);
      unflattenKeylistIntoAnswers([$intern_48], $intern_54 + $intern_55);
      strongName = answers[computePropValue($intern_46)];
      var idx = strongName.indexOf($intern_56);
      if (idx != -1) {
        softPermutationId = parseInt(strongName.substring(idx + $intern_8), $intern_18);
        strongName = strongName.substring($intern_7, idx);
      }
    }
     catch (e) {
    }
    html.__softPermutationId = softPermutationId;
    return computeUrlForResource(strongName + $intern_57);
  }

  function loadExternalStylesheets(){
    if (!$wnd.__gwt_stylesLoaded) {
      $wnd.__gwt_stylesLoaded = {};
    }
    function installOneStylesheet(stylesheetUrl){
      if (!__gwt_stylesLoaded[stylesheetUrl]) {
        var l = $doc.createElement($intern_58);
        l.setAttribute($intern_59, $intern_60);
        l.setAttribute($intern_61, computeUrlForResource(stylesheetUrl));
        $doc.getElementsByTagName($intern_62)[$intern_7].appendChild(l);
        __gwt_stylesLoaded[stylesheetUrl] = true;
      }
    }

    sendStats($intern_63, $intern_1);
    installOneStylesheet($intern_64);
    sendStats($intern_63, $intern_65);
  }

  processMetas();
  html.__moduleBase = computeScriptBase();
  activeModules[$intern_4].moduleBase = html.__moduleBase;
  var filename = getCompiledCodeFilename();
  if ($wnd) {
    var devModePermitted = !!($wnd.location.protocol == $intern_66 || $wnd.location.protocol == $intern_67);
    $wnd.__gwt_activeModules[$intern_4].canRedirect = devModePermitted;
    function supportsSessionStorage(){
      var key = $intern_68;
      try {
        $wnd.sessionStorage.setItem(key, key);
        $wnd.sessionStorage.removeItem(key);
        return true;
      }
       catch (e) {
        return false;
      }
    }

    if (devModePermitted && supportsSessionStorage()) {
      var devModeKey = $intern_69;
      var devModeUrl = $wnd.sessionStorage[devModeKey];
      if (!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/.*$/.test(devModeUrl)) {
        if (devModeUrl && (window.console && console.log)) {
          console.log($intern_70 + devModeUrl);
        }
        devModeUrl = $intern_14;
      }
      if (devModeUrl && !$wnd[devModeKey]) {
        $wnd[devModeKey] = true;
        $wnd[devModeKey + $intern_71] = computeScriptBase();
        var devModeScript = $doc.createElement($intern_19);
        devModeScript.src = devModeUrl;
        var head = $doc.getElementsByTagName($intern_62)[$intern_7];
        head.insertBefore(devModeScript, head.firstElementChild || head.children[$intern_7]);
        return false;
      }
    }
  }
  loadExternalStylesheets();
  sendStats($intern_0, $intern_65);
  installScript(filename);
  return true;
}

html.succeeded = html();
