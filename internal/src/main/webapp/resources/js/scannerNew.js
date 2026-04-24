
var DWObject;

var idUserIndex     = -1;
var idObjectIndex   = -2;
var codeObjectIndex = -3;
var scanFileName = "new";

var certIssuer =-1;
var certSN = -1; 
//igg
function LoadDWT(clientId,idUserIndex_,idObjectIndex_,codeObjectIndex_) { //alert(clientId); 
	//document.getElementById("source" + clientId).options.add(new Option("fdf", 0));
	
	idUserIndex     = idUserIndex_;
	idObjectIndex   = idObjectIndex_;
	codeObjectIndex = codeObjectIndex_;
	
    Dynamsoft.WebTwainEnv.Containers = [];
    try{
    Dynamsoft.WebTwainEnv.Containers.push({ContainerId: 'dwtcontrolContainer', Width: 200, Height: 250});
    } catch(e) {
    	alert (e);
    }
    Dynamsoft.WebTwainEnv.Load();// load all the resources of Dynamic Web TWAIN
   // alert("1"); 
    Dynamsoft.WebTwainEnv.RegisterEvent('OnWebTwainReady',function () {
            DWObject = Dynamsoft.WebTwainEnv.GetWebTwain('dwtcontrolContainer');
            if (DWObject) { 
            	
            	//DWObject.IfAllowLocalCache = true; //enable disk caching
            	
                var count = DWObject.SourceCount; // Populate how many sources are installed in the system
                
                if (count == 0 && Dynamsoft.Lib.env.bMac) {
                    DWObject.CloseSourceManager();
                    DWObject.ImageCaptureDriverType = 0;
                    DWObject.OpenSourceManager();
                    count = DWObject.SourceCount;
                }
                for (var i = 0; i < count; i++) {
                    document.getElementById("source" + clientId).options.add(new Option(DWObject.GetSourceNameItems(i), i));  // Add the sources in a drop-down list
                   
                }
            }
        }
    );
   
   // alert("2"); 
    if(browser()=='IE'){
    	document.getElementById('confirmPannelIe').style.display = 'block';
    } else {
    	document.getElementById('confirmPannelIe').style.display = 'none';
    }
    
   // alert("3");
}

function unloadDWT() {//alert("unloadDWT2 -start"); 
    Dynamsoft.WebTwainEnv.Unload();// Unload all the resources of Dynamic Web TWAIN
  //  alert("unloadDWT -end"); 
}


function AcquireImage(clientId) {
    
	if (DWObject) {
    	DWObject.SelectSourceByIndex(document.getElementById("source"+clientId).selectedIndex);
        DWObject.OpenSource();
        DWObject.IfDisableSourceAfterAcquire = true;
    //    DWObject.IfShowIndicator = true;
        //Pixel type
        if (document.getElementById(clientId+":PT:0").checked){ //alert("PT:0"); 
            DWObject.PixelType = EnumDWT_PixelType.TWPT_BW;
        } else if (document.getElementById(clientId+":PT:1").checked){ //alert("PT:1"); 
            DWObject.PixelType = EnumDWT_PixelType.TWPT_GRAY;
		} else if (document.getElementById(clientId+":PT:2").checked){//alert("PT:2"); 
            DWObject.PixelType = EnumDWT_PixelType.TWPT_RGB;
		}
        //If auto feeder
        if (document.getElementById(clientId+":ADF_input").checked){
            DWObject.IfFeederEnabled = true; //alert("ADF:1"); 
        }else{
            DWObject.IfFeederEnabled = false; //alert("ADF:2"); 
        }
        //If show UI
        if (document.getElementById(clientId+":ShowUI_input").checked){
            DWObject.IfShowUI = true;  //alert("ShowUI:1"); 
        } else {
            DWObject.IfShowUI = false;  //alert("ShowUI:2"); 
        }
        
        //======Enable Duplex
		if (DWObject.Duplex != 0){
	        	DWObject.IfDuplexEnabled = true ;//Enable duplex
		}
		//======Skip blank pages start
		DWObject.IfAutoDiscardBlankpages = true;
		//*Use capability negotiation
		DWObject.Capability = EnumDWT_Cap.ICAP_AUTODISCARDBLANKPAGES;
		DWObject.CapType = EnumDWT_CapType.TWON_ONEVALUE;
		DWObject.CapValue = -1;//Auto
		// ===== Skip Blank Pages End 
        
        
        //Resolution    j_idt332:Resolutionj_idt332_label
        DWObject.Resolution = parseInt(document.getElementById(clientId+":Resolution_input").value);
        
       // alert("Resolution:  ->"+ DWObject.Resolution);
        
        //j_idt332:scanFileName_j_idt332
        scanFileName = document.getElementById(clientId+":scanFileName").value
        // 
        //alert("scanFileName:  ->"+scanFileName); 
        
        if(browser()=='IE'){
        	 DWObject.AcquireImage();
        } else {
        	DWObject.AcquireImage(OnSuccess, OnFailure);
        }
    } 
}                                 

function AcquireImageSign(clientId ,certIssuer_ ,certSN_){
	
	certIssuer = certIssuer_;
	certSN = certSN_;
	
	//alert(certSN);
	
	AcquireImage(clientId);
}


function OnSuccess() { //alert("OnSuccess");
		
	if (!checkIfImagesInBuffer()) {alert('Buffer is empty');
	    return;
	}
	
	
	if(certSN!=-1){
			
			//for test
	//		DWObject.GetSelectedImagesSize(EnumDWT_ImageType.IT_PDF);
	//		var  imagedata_ =  DWObject.SaveSelectedImagesToBase64Binary();
	//		console.log(imagedata_);
	//		HTTPUploadThroughPostIndex(imagedata_);
			
		gotoSignData();
	} else {
	
	
		/** upload*/
		var strHTTPServer = location.hostname; //The name of the HTTP server. 
		var CurrentPathName = unescape(location.pathname);
		var CurrentPath = CurrentPathName.substring(0, CurrentPathName.lastIndexOf("/") + 1);
		var strActionPage = CurrentPath + "uploadScannerServlet?idUser="+idUserIndex+"&idObj="+idObjectIndex+"&codeObject="+codeObjectIndex;
		  //alert(strActionPage);
		var ssl = false;
		
		var prot = location.protocol;
		if(prot === 'https:'){
			ssl = true;
		}
		
		DWObject.IfSSL = ssl; // Set whether SSL is used
		
		DWObject.HTTPPort = location.port == "" ? (ssl?443:80) : location.port;
		
		
		// Upload all the images in Dynamic Web TWAIN viewer to the HTTP server as a PDF file asynchronously
		if(browser()=='IE'){
			if(DWObject.HTTPUploadAllThroughPostAsPDF( strHTTPServer, strActionPage, scanFileName+".pdf")){
				OnHttpUploadSuccess();
			} else {
				OnHttpUploadFailure("","","");
			}
	    } else {
	    	console.log("port:"+location.port);
	    	console.log("DWObject.HTTPPort: "+DWObject.HTTPPort)
	    	DWObject.HTTPUploadAllThroughPostAsPDF( strHTTPServer, strActionPage, scanFileName+".pdf",OnHttpUploadSuccess,OnHttpUploadFailure); 
	    }
	
	}
}
//
function OnFailure(errorCode, errorString) {        	
    alert(errorString);
    certIssuer =-1;
    certSN = -1; 
}

//
function OnHttpUploadSuccess() {//alert("OnHttpUploadSuccess");
	//console.log('successful');
	unloadDWT();
	hideModalScannerDs(); //TODO  da se pomisli kak ще се презареди списъка в компонентата за файловете
	//PF('scannerModalDs').hide();
	
	certIssuer =-1;
    certSN = -1; 
}

//
function OnHttpUploadFailure(errorCode, errorString, sHttpResponse) {
    alert(errorString + sHttpResponse);
}

//
function LoadImage() {
    if (DWObject) {
        DWObject.IfShowFileDialog = true;
        DWObject.LoadImageEx("", EnumDWT_ImageType.IT_ALL, OnSuccess, OnFailure);
    }
}

//
function checkIfImagesInBuffer() {
    if (DWObject.HowManyImagesInBuffer == 0) {
        //alert("There is no image in buffer.<br />")
        return false;
    }
    else
        return true;
}

function browser() {
    // Return cached result if avalible, else get result then cache it.
    if (browser.prototype._cachedResult)
        return browser.prototype._cachedResult;

    // Opera 8.0+
    var isOpera = (!!window.opr && !!opr.addons) || !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;

    // Firefox 1.0+
    var isFirefox = typeof InstallTrigger !== 'undefined';

    // Safari 3.0+ "[object HTMLElementConstructor]" 
    var isSafari = /constructor/i.test(window.HTMLElement) || (function (p) { return p.toString() === "[object SafariRemoteNotification]"; })(!window['safari'] || safari.pushNotification);

    // Internet Explorer 6-11
    var isIE = /*@cc_on!@*/false || !!document.documentMode;

    // Edge 20+
    var isEdge = !isIE && !!window.StyleMedia;

    // Chrome 1+
    var isChrome = !!window.chrome && !!window.chrome.webstore;

    // Blink engine detection
    var isBlink = (isChrome || isOpera) && !!window.CSS;

    return browser.prototype._cachedResult =
        isOpera ? 'Opera' :
        isFirefox ? 'Firefox' :
        isSafari ? 'Safari' :
        isChrome ? 'Chrome' :
        isIE ? 'IE' :
        isEdge ? 'Edge' :
        "Don't know";
}


function gotoSignData(){
	
	DWObject.GetSelectedImagesSize(EnumDWT_ImageType.IT_PDF);
	var  imagedata = DWObject.SaveSelectedImagesToBase64Binary();
	//console.log(imagedata);
	
	var data = JSON.stringify({"inputType":"file","input": "" + imagedata + "" ,"issuerCN":"" + certIssuer +  "","certificateSN":"" + certSN +  "","hashAlgorithm":"sha256","signatureType":"6","inputFile":"19-27.pdf","outputFile":"true","version":"1.0","SignatureReason":" ","SignaturePage":"0","SignatureOffsetX":"0","SignatureOffsetY":"0"}),    

	xhttp = new XMLHttpRequest();
	xhttp.open("POST", "http://localhost:5000/api/signPDF", true);
    xhttp.setRequestHeader('Content-type','application/json; charset=utf-8');
	xhttp.send(data);
    xhttp.onreadystatechange = function () {
        if (xhttp.readyState == 4 && xhttp.status == "200") {
            var resultData = JSON.parse(xhttp.responseText);
	          if(resultData.status == 200){
	        	 //console.log(resultData.signature);	
	        	 HTTPUploadThroughPostIndex(resultData.signature ,'');
				} else if(resultData.status == 400){
					alert(" Грешка при подписването на файла !!!" + resultData.status + resultData.description);
				} else if(resultData.status == 404){
					alert(" Не е намерен адреса на програмата за подписване !!!" + resultData.status + resultData.description);
				} else if(resultData.status == 500){
					alert(" Неуспешна верификация на ел.подпис !!!" + resultData.status + resultData.description);
				}
        } 

    };
	xhttp.onerror = function(){
		  var errText;
		  	if (this.status==0) {
				alert('Няма връзка с програмата за подписване!');
			} else if(this.status==404) {
				alert('Не е намерен адреса на програмата за подписване!');
			} else if(this.status==500) {
				alert('Грешка в програмата за подписване!');
			} else if(this.statusText=='parsererror') {
				alert('Грешка при прочитане на върнатия резултат!');
			} else if(this.statusText=='timeout'){
				alert('Изтече времето за връзка с програмата за подписване!');
			} else {
				alert('Грешка!'+this.responseText);
			}
	};
}

function HTTPUploadThroughPostIndex(imagedata_ ){
	var formData = new FormData();
	
	//let blob = new Blob([imagedata_],{type: 'application/pdf'});
	//formData.append("imagedata"   ,blob ,scanFileName+".pdf");
	
	formData.append("imagedata"   ,imagedata_);
	formData.append("idObj"       , idObjectIndex);
	formData.append("codeObject"  , codeObjectIndex);
	formData.append("scanFileName", scanFileName+".pdf");
	formData.append("idUser"      , idUserIndex);
	formData.append("signedPdf"   , "1");
	
	var xhr = new XMLHttpRequest();
	xhr.open("POST", 'uploadScannerSignServlet', true);
    xhr.send(formData);
	xhr.onreadystatechange = function() { // Call a function when the state changes.
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            // Request finished. Do processing here.
        	OnHttpUploadSuccess();
        } 
    };
    xhr.onerror = function(){
		alert('Грешка!'+this.responseText);
		OnHttpUploadFailure();
	};
}

function actionScanBnt(clientId){
	let elem = document.getElementById("sign"+clientId);
	if (elem && elem.checked){
		 PF('scannerSignsPanelDs').show();
	} else {
		 AcquireImage(clientId);
	}
}
