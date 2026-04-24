
var idUserIB     = -1;
var idObjectIB   = -2;
var codeObjectIB = -3;

var certIssuerIB = "";
var certSNIB = ""; 

var sessionId = "";

var httpUrlSystem = "";

//igg
function LoadIB(clientId,idUserIndex_,idObjectIndex_,codeObjectIndex_) { //alert(clientId); 
	
//	document.getElementById("source" + clientId).options.add(new Option("fdf", 0));
//	document.getElementById("source" + clientId).options.add(new Option("alabala", 1));

	try{
		var sIBdata =  $.cookie('scannerIBdata');
		//console.log(sIBdata);
		sIBdata = JSON.parse(sIBdata);
		PF('resolutionIBWV').jq.find('input:radio[value="'+sIBdata.resolution+'"]').parent().next().trigger('click.selectOneRadio');

		PF('ptibWV').jq.find('input:radio[value="'+sIBdata.ptType+'"]').parent().next().trigger('click.selectOneRadio');

		//PF('resolutionIBWV').select($('.ui-radiobutton-box').first())
		if(sIBdata.adf=='1'){
			PF('adfWV').check();
		} else {
			PF('adfWV').uncheck();
		}

		if(sIBdata.duplex=='1'){
			PF('duplexWV').check();
		} else {
			PF('duplexWV').uncheck();
		}

		if(sIBdata.showUi=='1'){
			PF('showUIWV').check();
		} else {
			PF('showUIWV').uncheck();
		}

		if(sIBdata.signel=='1'){
			PF('signWV').check();
		} else {
			PF('signWV').uncheck();
		}
	} catch(e) {
		console.log(e);
	}


	idUserIB     = idUserIndex_;
	idObjectIB   = idObjectIndex_;
	codeObjectIB = codeObjectIndex_;
   
    
	httpUrlSystem = location.origin; //The origin property returns the protocol, hostname and port number of a URL. 
									 //If the port number is not specified in the URL (or if it is the scheme's default port - like 80, or 443), some browsers will not display the port number.
	var currentPathName = unescape(location.pathname);
	var currentPath = currentPathName.substring(0, currentPathName.lastIndexOf("/") + 1);
	httpUrlSystem = httpUrlSystem+""+ currentPath
	
	httpUrlSystem = httpUrlSystem.replace("pages", "rest");
	
	console.log(httpUrlSystem);
	
	document.getElementById(clientId+":paneldata").style.visibility ="hidden";
  	controlScannBnt(clientId ,true);
  	
	xhttp = new XMLHttpRequest();
	  xhttp.open("GET", "http://localhost:5000/api/Scan", true);
	  
	//  xhttp.withCredentials = true;  //29,4,2022 не беше коментирано и сега го коментирам за тест интересно че съм го поставил само тук
//	  xhttp.setRequestHeader('Access-Control-Allow-Headers', '*');
//	  xhttp.setRequestHeader('Content-type', 'application/json');
//	  xhttp.setRequestHeader('Access-Control-Allow-Origin', '*');
	  
	  
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {//console.log(this.responseText);
	    	var dataObj = JSON.parse(this.responseText);
      
	      	for (var i = 0; i < dataObj.scannerList.length; i++) {
	      		
				document.getElementById("sourceIB" + clientId).options.add(new Option(dataObj.scannerList[i], i));  // Add the sources in a drop-down list
		    }
	      	
	      	document.getElementById(clientId+":paneldata").style.visibility ="visible";
	      	document.getElementById(clientId+":panelprogress").style.visibility ="hidden";
	      	controlScannBnt(clientId ,false);
	    }
	  };
	  xhttp.send();
	  xhttp.onerror = function(){
		  var errText;
			if (this.status==0) {
				errText = 'Грешка = Няма връзка с програмата за сканиране (списък скенери)!';
				console.log(errText);
				//alert(errText);
				PF('scannerModalIB').hide();
				PF('noScanningProgram').show();
			} else if(this.status==404) {
				errText = 'Грешка = Не е намерен адреса на програмата за сканиране (списък скенери)!';
				console.log(errText);
				alert(errText);
			} else if(this.status==500) {
				errText = 'Грешка = в програмата за сканиране (списък скенери)!';
				console.log(errText);
				alert(errText);
			} else if(this.statusText=='parsererror') {
				errText = 'Грешка = при прочитане на върнатия резултат (списък скенери)!';
				console.log(errText);
				alert(errText);
			} else if(this.statusText=='timeout'){
				errText = 'Грешка = Изтече времето за връзка с програмата за сканиране (списък скенери)!';
				console.log(errText);
				alert(errText);
			} else {
				errText = 'Грешка = !'+this.responseText;
				console.log(errText);
				alert(errText);
			}
	  };
	
	
}


function AcquireImageIB(clientId ,stage) { 
    
	PF('nextLastScanPanelIB').hide();
	
	controlScannBnt(clientId ,true);
	
	var ptType = ""; //RGB, BW, Grey   
	//Pixel type
    if (document.getElementById(clientId+":PTIB:0").checked){
    	ptType = "BW";
    } else if (document.getElementById(clientId+":PTIB:1").checked){
        ptType = "Grey";
    } else if (document.getElementById(clientId+":PTIB:2").checked){
    	ptType = "RGB";
    }
    //If auto feeder
    var adf = "N";
    if (document.getElementById(clientId+":ADFIB_input").checked){
    	 adf = "Y";
    }
    //If show UI
    var showUi = "N";
    if (document.getElementById(clientId+":ShowUIIB_input").checked){
    	showUi = "Y";
    }
    
   //If duplex
    var duplex = "N";
    if (document.getElementById(clientId+":DuplexIB_input").checked){
    	duplex = "Y";
    }
    
 //   var resolution =  document.getElementById(clientId+":ResolutionIB_input").value; //беше селект меню но го сменихме с радио бутони
	
	var resolution = "100";
	
	if (document.getElementById(clientId+":ResolutionIB:0").checked){
    	resolution = "100";
    } else if (document.getElementById(clientId+":ResolutionIB:1").checked){
        resolution = "150";
    } else if (document.getElementById(clientId+":ResolutionIB:2").checked){
    	resolution = "200";
    }else if (document.getElementById(clientId+":ResolutionIB:3").checked){
    	resolution = "300";
    }

	var fileName   =  document.getElementById(clientId+":scanFileNameIB").value;
	
	//sourceIB
	var sourceIB = document.getElementById("sourceIB"+clientId);
	let x   = sourceIB.selectedIndex;
	
	if(x !== -1){
		
		var dsn = sourceIB.options[x].text;
	
		var data = JSON.stringify({"dsn":""+dsn+"",
				  					 "sessionId":""+sessionId+"",
				  					 "stage":""+stage+"",
				  					 "docName":""+fileName+"",
				  					 "docFormat":"pdf",
				  					 "previewFormat":"",
				  					 "showUI":""+showUi+"",
				  					 "adf":""+adf+"",
				  					 "duplex":""+duplex+"",
				  					 "resolution":""+resolution+"",
				  					 "pixelType":""+ptType+"",
				  					 "transferMode":"http", 
				  					 "httpUrl": httpUrlSystem+"uploadScannerServletIB",
				  					 "httpUser":""+idUserIB+"",
				  					 "httpPwd":"",
				  					 "issuerCN":""+certIssuerIB+"",
				  					 "certificateSN":""+certSNIB+"",
				  					 "signatureReason":"",
				  					 "CodeObj":""+codeObjectIB ,
				  					 "IdObj":""+idObjectIB});  
			
		console.log(data);
		
		  xhttp = new XMLHttpRequest();
		  xhttp.open("POST", "http://localhost:5000/api/Scan", true);
		  xhttp.setRequestHeader('Content-type','application/json; charset=utf-8');
		  xhttp.send(data);
		  xhttp.onreadystatechange = function() {
			  if (xhttp.readyState == 4 && xhttp.status == "200") {
		            var resultData = JSON.parse(xhttp.responseText);
			        if(resultData.status == 200){
			        	 console.log(resultData.sessionId);	
			        	
			        	 if(stage !=='end'){
			        		 sessionId = resultData.sessionId;
			        		 
			        		 if (document.getElementById(clientId+":multiScanIB_input").checked){
			        			PF('nextLastScanPanelIB').show();
			        		 } else {
			        			 AcquireImageIB(clientId ,"end");
			        		 }
			        		
			        	 } else {
			        		 console.log(resultData);
			        		 OnHttpUploadSuccessIB(clientId); 
			        	 }
			        	 
					} else if(resultData.status == 400){
							console.log(resultData);
							alert(resultData.description);
							OnHttpUploadFailureIB(clientId);
					} else if(resultData.status == 404){
							console.log(resultData);
							alert(resultData.status + resultData.description);
							OnHttpUploadFailureIB(clientId);
					} else if(resultData.status == 500){
							console.log(resultData);
							alert(resultData.description);
							OnHttpUploadFailureIB(clientId);
					}
		      } 
		  };	 
		  xhttp.onerror = function(){
			  var errText;
				if (this.status==0) {
					errText = 'Грешка = Няма връзка с програмата за сканиране (списък скенери)!';
					console.log(errText);
					alert(errText);
				} else if(this.status==404) {
					errText = 'Грешка = Не е намерен адреса на програмата за сканиране (списък скенери)!';
					console.log(errText);
					alert(errText);
				} else if(this.status==500) {
					errText = 'Грешка = в програмата за сканиране (списък скенери)!';
					console.log(errText);
					alert(errText);
				} else if(this.statusText=='parsererror') {
					errText = 'Грешка = при прочитане на върнатия резултат (списък скенери)!';
					console.log(errText);
					alert(errText);
				} else if(this.statusText=='timeout'){
					errText = 'Грешка = Изтече времето за връзка с програмата за сканиране (списък скенери)!';
					console.log(errText);
					alert(errText);
				} else {
					errText = 'Грешка = !'+this.responseText;
					console.log(errText);
					alert(errText);
				}
		  };
	
	} else {
		console.log("няма намерени скенер");
		alert("Няма намерени скенер!");
		
	}

 
}                                 


function AcquireImageSignIB(clientId ,certIssuer_ ,certSN_){
	
	certIssuerIB = certIssuer_;
	certSNIB = certSN_;
	
	//alert(certSN);
	
	AcquireImageIB(clientId ,"first");
}


//
function OnHttpUploadSuccessIB(clientId) {
	console.log('successful');
	
	controlScannBnt(clientId, false);	
	hideModalScannerIB();
	
	certIssuerIB ="";
    certSNIB = ""; 
    sessionId ="";
    
}

//
function OnHttpUploadFailureIB(clientId) {
	console.log('OnHttpUploadFailure ');
	
	controlScannBnt(clientId, false);
	
	PF('nextLastScanPanelIB').hide();
	
	certIssuerIB ="";
    certSNIB = ""; 
    sessionId ="";
}


function actionScanBntIB(clientId){

	//----------------------------------------------------------
	var cookiedata = ' { "ptType": '; //RGB, BW, Grey
	//Pixel type
	if (document.getElementById(clientId+":PTIB:0").checked){
		cookiedata += '"BW"';
	} else if (document.getElementById(clientId+":PTIB:1").checked){
		cookiedata += '"Grey"';
	} else if (document.getElementById(clientId+":PTIB:2").checked){
		cookiedata += '"RGB"';
	} else {
		cookiedata += '"Grey"';
	}

	//If auto feeder
	cookiedata += ',"adf": ';
	if (document.getElementById(clientId+":ADFIB_input").checked){
		cookiedata += '"1"';
	} else {
		cookiedata += '"0"';
	}

	//If show UI
	cookiedata += ',"showUi": ';
	if (document.getElementById(clientId+":ShowUIIB_input").checked){
		cookiedata += '"1"';
	} else {
		cookiedata += '"0"';
	}

	//If duplex
	cookiedata += ',"duplex": ';
	if (document.getElementById(clientId+":DuplexIB_input").checked){
		cookiedata += '"1"';
	} else {
		cookiedata += '"0"';
	}

	cookiedata += ',"resolution": ';
	if (document.getElementById(clientId+":ResolutionIB:0").checked){
		cookiedata += '"100"';
	} else if (document.getElementById(clientId+":ResolutionIB:1").checked){
		cookiedata += '"150"';
	} else if (document.getElementById(clientId+":ResolutionIB:2").checked){
		cookiedata += '"200"';
	}else if (document.getElementById(clientId+":ResolutionIB:3").checked){
		cookiedata += '"300"';
	} else {
		cookiedata += '"150"';
	}

	cookiedata += ',"signel": ';
	if (document.getElementById(clientId+":signIB_input").checked){
		cookiedata += '"1"';
	} else {
		cookiedata += '"0"';
	}

	cookiedata +='}';

	$.cookie('scannerIBdata', cookiedata, {path: '/',expires :365,samesite: 'strict'});

	//----------------------------------------------------------

	let elem = document.getElementById(clientId+":signIB_input");
	if (elem && elem.checked){ 
		 createCertListXMLObj(clientId);
		 PF('scannerSignsPanelIB').show();
	} else {
		 AcquireImageIB(clientId,"first"); 
	}
}

function controlScannBnt(clientId,disabledBnt){
	try{
		var bntS = document.getElementById(clientId+":scannBntIB") 
		bntS.disabled = disabledBnt;
		
		if(disabledBnt)
			document.getElementById(clientId+":panelprogress").style.visibility ="visible";
		else 
			document.getElementById(clientId+":panelprogress").style.visibility ="hidden";
		
	} catch (e) {
		console.log("Not fount "+clientId+":scannBntIB");
	}
}

function createCertListXMLObj(clientId){
	
	  xhttp = new XMLHttpRequest();
	  xhttp.open("GET", "http://localhost:5000/api/certList", true);
	  xhttp.onreadystatechange = function() {
	    if (xhttp.readyState == 4 && xhttp.status == 200) {
	    	//console.log(xhttp.responseText);
	    	var certsList = JSON.parse(xhttp.responseText);
	    	//console.log(certsList);
//        	var certNames = [];
//        	for (var i = 0; i < certsList.length; i++) {
//        		certNames[i] =  certsList[i].certCN + "/" + getIssuerName(certsList[i].issuerName) + "/" + certsList[i].serialNumber;
//			}
	    	
	    	var _table_ = document.createElement('table'),
	        _tr_ = document.createElement('tr'),
	        _th_ = document.createElement('th'),
	        _td_ = document.createElement('td'),
	        _button_ = document.createElement('button');
	    	
	    	_button_.setAttribute('type', 'button');
	    	_button_.setAttribute('role', 'button');
	    	_button_.setAttribute('class','ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left');
	    	
	    	var table = _table_.cloneNode(false),
	    	trh = _tr_.cloneNode(false);
	    	
	    	table.setAttribute("border","0");
	    	table.setAttribute("width","100%");
	    	var th = _th_.cloneNode(false);
            th.appendChild(document.createTextNode(' '));
            trh.appendChild(th);
            
            th = _th_.cloneNode(false);
            th.appendChild(document.createTextNode('Име'));
            trh.appendChild(th);
            
            th = _th_.cloneNode(false);
            th.appendChild(document.createTextNode('Издател'));
            trh.appendChild(th);
            
            th = _th_.cloneNode(false);
            th.appendChild(document.createTextNode('Сериен номер'));
            trh.appendChild(th);
            
            table.appendChild(trh);
            
		    for (var i = 0; i < certsList.length; i++) {
		         var tr = _tr_.cloneNode(false);
		        
		         var td = _td_.cloneNode(false);
		         
		         var button = _button_.cloneNode(false);
		         
		         let issuerName  = getIssuerName(certsList[i].issuerName);
		         
		         button.setAttribute('onclick', "AcquireImageSignIB('"+clientId+"' ,'"+issuerName+"' ,'"+certsList[i].serialNumber +"'); PF('scannerSignsPanelIB').hide();");
		        
		       
		         var icon = document.createElement('span');
		         icon.setAttribute('class','ui-button-icon-left ui-icon ui-c fas fa-file-signature');
		         button.appendChild(icon);
		         var textbnt = document.createElement('span');
		         textbnt.setAttribute('class','ui-button-text ui-c');
		         textbnt.appendChild(document.createTextNode('Подпис'));
		         button.appendChild(textbnt);
		         
		         td.appendChild(button);
	             tr.appendChild(td);
		         
		         td = _td_.cloneNode(false)
		         td.appendChild(document.createTextNode(certsList[i].certCN || ''));
	             tr.appendChild(td);
	             
	             td = _td_.cloneNode(false);
	            td.appendChild(document.createTextNode( issuerName || ''));
	             tr.appendChild(td);
	             
	             td = _td_.cloneNode(false);
		         td.appendChild(document.createTextNode(certsList[i].serialNumber || ''));
	             tr.appendChild(td);
		        
		         table.appendChild(tr);
		    }
		    
		    //----
		    try{
			    var div_content = document.getElementById(clientId+":еlSignsPanelIB_content"); 
			    
			    while (div_content.firstChild) {
			    	div_content.removeChild(div_content.firstChild);
			    }
			    
			    div_content.appendChild(table);
		    } catch (e) {
				console.log("Not fount "+clientId+":еlSignsPanelIB_content");
			}
        	
	    }
	  };
	  xhttp.send();
	  xhttp.onerror = function(){
		  var errText;
			if (this.status==0) {
				//alert('You are offline!!\n Please Check Your Network.');
				errText = 'Грешка = Няма връзка с програмата за подписване!';
				console.log(errText);
			} else if(this.status==404) {
				//alert('Requested URL not found.');
				errText = 'Грешка = Не е намерен адреса на програмата за подписване!';
				console.log(errText);
			} else if(this.status==500) {
				//alert('Internel Server Error.');
				errText = 'Грешка = в програмата за подписване!';
				console.log(errText);
			} else if(this.statusText=='parsererror') {
				//errText = 'Error.\nParsing JSON Request failed.');
				errText = 'Грешка = при прочитане на върнатия резултат!';
				console.log(errText);
			} else if(this.statusText=='timeout'){
				//errText = 'Request Time out.');
				errText = 'Грешка = Изтече времето за връзка с програмата за подписване!';
				console.log(errText);
			} else {
				//errText = 'Unknow Error.\n'+x.responseText);
				errText = 'Грешка = !'+this.responseText;
				console.log(errText);
			}
	  };
	
}

function getIssuerName(issuerCN){
    if (issuerCN.length > 0) {
        var i = issuerCN.indexOf("CN=");
        var k = issuerCN.indexOf(',', i);
        if (k > i) {                                             //29.01.2020 След CN= е намерена запетая.
            issuerCN = issuerCN.substring(i + 3, k);
        }
        else {                                                   //29.01.2020 След CN= НЕ е намерена запетая. Вземам всичко до края
            issuerCN = issuerCN.substring(i + 3);
        }
    }
    return issuerCN;
}