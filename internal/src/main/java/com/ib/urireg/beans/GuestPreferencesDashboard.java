/*
 * Copyright 2009-2014 PrimeTek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ib.urireg.beans;

import com.ib.indexui.utils.JSFUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@SessionScoped
public class GuestPreferencesDashboard implements Serializable {

    /**
	 *
	 */
	private static final long serialVersionUID = 5644709981503440640L;


	private boolean autoDisplay;
	private boolean panelDisplay;
	private boolean modalDisplay;

    @PostConstruct
    public void init() {

    	setAutoDisplay(true); //po podrazbirane


    	//да изчетем бисквитката да видим какво е избрал потребителят
    	String val = JSFUtils.readCookie("dashboard_auto");


    //	System.out.println("dashboard_auto --> " +val);

    	if(val!=null && !val.isEmpty()) {
    		int key = Long.valueOf(val).intValue();

    		switch(key) {

    			case 1:
    				setAutoDisplay(true);
    			break;
    			case 2:
    				setPanelDisplay(true);
    			break;
    			case 3:
    				setModalDisplay(true);
    			break;

    		}

    	}
    }

	public boolean isAutoDisplay() {
		return autoDisplay;
	}

	public void setAutoDisplay(boolean autoDisplay) {
		this.autoDisplay = autoDisplay;
		this.panelDisplay = false;
		this.modalDisplay = false;
	}

	public boolean isPanelDisplay() {
		return panelDisplay;
	}

	public void setPanelDisplay(boolean panelDisplay) {
		this.panelDisplay = panelDisplay;
		this.autoDisplay = false;
		this.modalDisplay = false;
	}

	public boolean isModalDisplay() {
		return modalDisplay;
	}

	public void setModalDisplay(boolean modalDisplay) {
		this.modalDisplay = modalDisplay;
		this.autoDisplay = false;
		this.panelDisplay = false;

	}



}
