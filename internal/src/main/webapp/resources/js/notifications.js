function timedCount() {
            window.location.href = "../sessionExpired2.xhtml";
            return;
        }
//Функции използвани за нотификациите

function setConnectedKG(channel){
		console.log("setConnectedAA:channel="+channel);
		setUserRegistrature();
}
function setDisconnected(code, channel, event) {
		console.log("setDisconnectedAA:"+code);
		/*		var currentStatus = document.getElementById('currentstatus');
				currentStatus.setAttribute("class","error") */ 
			
	    if (code == -1) {
	        // Web sockets not supported by client.
	    } else if (code == 1000) {
	        // Normal close (as result of expired session or view).
	    } else {
	        // Abnormal close reason (as result of an error).
	    }
	}
function myOnMessageMain(message){
console.log("myOnMessageMain:"+message);
if (message instanceof Object){
	console.log(message['title']);
	console.log(message['details']);
	console.log(message['severity']);
	console.log(message['messageType']);
	if(message['messageType']>0 ){	
		showKGMaain(message);
		upNumber();
	} else {
		
		if( message['messageType'] == -5555 ){
			console.log("DeloTransferReloadMess -5555 ");
			try {
				reloadDocDataFile();
			} catch(er) { 	console.log("reloadDocDataFile not found ") }
		} else if( message['messageType'] == -6666 ){ 
			console.log("DeloTransferReloadMess -6666 ");
			try {
				reloadDocDataFileTask([{name: 'param1', value: message['title']}]);
			} catch(er) { 	console.log("reloadDocDataFileTask not found ") }
		}
	}
	
	
	try {
		var dashboardPage = document.getElementById("dashboard");
		
		if (dashboardPage instanceof Object){
			
			
			switch(Math.abs(message['messageType'])) {
			  case 1111:
			  case 9:
				  loadCountDeloDocSec();
				  break;
			  case 2222:
			  case 18:
			  case 5:
			  case 6:
			  case 7:
			  case 8:
			  case 10:
			  case 16:
			  case 18:
			  case 19:
			  case 20:
			  case 21:
//			  case 22:
//			  case 23:
//			  case 24:
			  case 25:
			  case 26:
				  loadCountDocSec();
				  break;
			  case 3333:
			  case 19:
			  case 27:
			  case 28:
			  case 29:
			  case 32:
			  case 33:
			  case 34:
				  	loadCountTaskSec();
				    break;
			  default:
			    // code block
			} 
		
			
		
		} else {
			console.log("dashboardPage is not Object !!!")
		}
		
	} catch(err) { 	console.log("grymna loadCountDocSec ") }
	
}else {
	console.log("message is not Object !!!")
}

}

function upNumber() {
   var x=document.getElementById("alibaba").getAttribute("data-count");
  document.getElementById("alibaba").setAttribute("data-count",Number(x)+1)

}

function downNumber() {
   var x=document.getElementById("alibaba").getAttribute("data-count");
  document.getElementById("alibaba").setAttribute("data-count",Number(x)-1)

}

function downNumberCustom(downcount) {
	   var x=document.getElementById("alibaba").getAttribute("data-count");
	  document.getElementById("alibaba").setAttribute("data-count",Number(x)-Number(downcount));
}

function clearNumber() {
	 document.getElementById("alibaba").setAttribute("data-count",0)

}

function showKGMaain(message){
	PF('growlWV').renderMessage({"summary":message['title'],
      "detail":message['details'],
      "severity":message['severity']});
}



function changeContentPosition(value) { //alert('changeContentPosition');
    var containerElement = $('.container');
    if (value) {
    	containerElement.addClass('auto');
    }
    else {
    	containerElement.removeClass('auto');
    }
}

function changeResolution(){
	if(window.innerWidth <= 1400){
		if(!displayMod){
			dsplayModalSelObjJS();
			displayMod = true
		}
	} else {
		if(displayMod){ 
			displayMod = false;
			dsplayModalSelObjJS();
		}
	}
}

