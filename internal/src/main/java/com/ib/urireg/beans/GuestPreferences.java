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
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Този клас пази настройките на потребителя за цветове,
 * стил на менюто и тъмна тема. Силно модифицирана версия
 * на класа, който идва по подразбиране с темплейта.
 *
 * Методите тук се извикват в config.xhtml
 *
 * @author n.kanev
 */
@Named("guestPreferences")
@SessionScoped
public class GuestPreferences implements Serializable {

    private static final long serialVersionUID = 5644709981503440640L;

    private static final String COOKIE_NAME_DARK_MODE = 		"dark_mode";
    private static final String COOKIE_NAME_MENU_HOR = 			"menu_hor";
    private static final String COOKIE_NAME_MENU_COLOR = 	    "menu_color";
    private static final String COOKIE_NAME_MENU_THEME =		"menu_theme";
    private static final String COOKIE_NAME_INPUT_STYLE = 		"input_style";
    private static final String COOKIE_NAME_COMP_THEME =		"comp_theme";
    private static final String COOKIE_PATH =                   "/";

    // Tъмна тема за цялото приложение
    private boolean darkMode = false;

    // Дали менюто да е отгоре или отстрани
    private boolean horizontalMenu = true;

    // Тъмно/светло меню
    private String menuColor = "light";

    // Цветова тема за менюто (идва от /mirage-layout/css/layout-blue-light.css...)
    private String menuTheme = "blue";

    // Стил за input компонентите - нормални или запълнени
    private String inputStyle = "outlined";

    // Цветова тема за компонентите (идва от index-themes.jar)
    private String componentTheme = "index-blue";

    private String containerPosition = ""; //auto

    private List<ComponentTheme> componentThemes = new ArrayList<>();
    private Map<String, List<MenuTheme>> menuColors = new HashMap<>();

    @PostConstruct
    public void init() {
        readCookieValues();

        componentThemes.add(new ComponentTheme("Blue", "index-blue", "#5E81AC"));
        componentThemes.add(new ComponentTheme("Green", "index-green", "#99CE6B"));
        componentThemes.add(new ComponentTheme("Yellow", "index-yellow", "#EBCB8B"));
        componentThemes.add(new ComponentTheme("Cyan", "index-cyan", "#88C0D0"));
        componentThemes.add(new ComponentTheme("Purple", "index-purple", "#B48EAD"));
        componentThemes.add(new ComponentTheme("Orange", "index-orange", "#D08770"));
        componentThemes.add(new ComponentTheme("Teal", "index-teal", "#88D0BD"));
        componentThemes.add(new ComponentTheme("Magenta", "index-magenta", "#BD69AE"));
        componentThemes.add(new ComponentTheme("Lime", "index-lime", "#B9BE7F"));
        componentThemes.add(new ComponentTheme("Brown", "index-brown", "#BE9B7F"));
        componentThemes.add(new ComponentTheme("Red", "index-red", "#F28686"));
        componentThemes.add(new ComponentTheme("Indigo", "index-indigo", "#8886F2"));

        /* ************* Light ******************* */
        List<MenuTheme> menuThemes = new ArrayList<>();
        menuThemes.add(new MenuTheme("Blue", "blue", "#5e81ac"));
        menuThemes.add(new MenuTheme("Green", "green", "#A3BE8C"));
        menuThemes.add(new MenuTheme("Yellow", "yellow", "#EBCB8B"));
        menuThemes.add(new MenuTheme("Cyan", "cyan", "#88C0D0"));
        menuThemes.add(new MenuTheme("Purple", "purple", "#B48EAD"));
        menuThemes.add(new MenuTheme("Orange", "orange", "#D08770"));
        menuThemes.add(new MenuTheme("Teal", "teal", "#88D0BD"));
        menuThemes.add(new MenuTheme("Magenta", "magenta", "#BD69AE"));
        menuThemes.add(new MenuTheme("Lime", "lime", "#B9BE7F"));
        menuThemes.add(new MenuTheme("Brown", "brown", "#BE9B7F"));
        menuThemes.add(new MenuTheme("Red", "red", "#F28686"));
        menuThemes.add(new MenuTheme("Indigo", "indigo", "#8886F2"));
        menuColors.put("light", menuThemes);

        /* ************* Dark ******************* */
        menuThemes = new ArrayList<>();
        menuThemes.add(new MenuTheme("Blue", "blue", "#5e81ac"));
        menuThemes.add(new MenuTheme("Green", "green", "#A3BE8C"));
        menuThemes.add(new MenuTheme("Yellow", "yellow", "#EBCB8B"));
        menuThemes.add(new MenuTheme("Cyan", "cyan", "#88C0D0"));
        menuThemes.add(new MenuTheme("Purple", "purple", "#B48EAD"));
        menuThemes.add(new MenuTheme("Orange", "orange", "#D08770"));
        menuThemes.add(new MenuTheme("Teal", "teal", "#88D0BD"));
        menuThemes.add(new MenuTheme("Magenta", "magenta", "#BD69AE"));
        menuThemes.add(new MenuTheme("Lime", "lime", "#B9BE7F"));
        menuThemes.add(new MenuTheme("Brown", "brown", "#BE9B7F"));
        menuThemes.add(new MenuTheme("Red", "red", "#F28686"));
        menuThemes.add(new MenuTheme("Indigo", "indigo", "#8886F2"));
        menuColors.put("dark", menuThemes);

    }

    private void readCookieValues() {
        String val = JSFUtils.readCookie(COOKIE_NAME_DARK_MODE); // true / false
        if(!val.isEmpty()) setDarkMode(Boolean.parseBoolean(val));

        val = JSFUtils.readCookie(COOKIE_NAME_MENU_HOR); // true / false
        if(!val.isEmpty()) setHorizontalMenu(Boolean.parseBoolean(val));

        val = JSFUtils.readCookie(COOKIE_NAME_MENU_COLOR); // "light" / "dark"
        if(!val.isEmpty()) setMenuColor(val);

        val = JSFUtils.readCookie(COOKIE_NAME_MENU_THEME); // "blue" / "yellow"...
        if(!val.isEmpty()) setMenuTheme(val);

        val = JSFUtils.readCookie(COOKIE_NAME_INPUT_STYLE); // // "filled" / "outlined"
        if(!val.isEmpty()) setInputStyle(val);

        val = JSFUtils.readCookie(COOKIE_NAME_COMP_THEME); // "index-blue" / "index-yellow"...
        if(!val.isEmpty()) setComponentTheme(val);
    }

    public void setDarkThemeFromLogin(boolean darkMode) {
        setDarkMode(darkMode);
        onColorSchemeChange();
    }

    /**
     * Вика се при смяна на Тъмна/Светла тема
     */
    public void onColorSchemeChange() {
        PrimeFaces.current().executeScript("PrimeFaces.MirageConfigurator.changeLayout('" + menuTheme + "', '" + componentTheme + "', " + darkMode + ")");
        JSFUtils.saveCookie(COOKIE_NAME_DARK_MODE, String.valueOf(isDarkMode()), COOKIE_PATH, 365, null);
        JSFUtils.saveCookie(COOKIE_NAME_MENU_COLOR, getMenuColor(), COOKIE_PATH, 365, null);
    }

    /**
     * Вика се при смяна на Хоризонтално/Вертикално меню
     */
    public void onMenuTypeChange() {
        PrimeFaces.current().executeScript("PrimeFaces.MirageConfigurator.changeMenuToHorizontal(" + horizontalMenu +")");
        JSFUtils.saveCookie(COOKIE_NAME_MENU_HOR, String.valueOf(isHorizontalMenu()), COOKIE_PATH, 365, null);
    }

    /**
     * Вика се при смяна на Тъмно/Светло меню
     */
    public void onMenuColorChange() {
        PrimeFaces.current().executeScript("PrimeFaces.MirageConfigurator.changeMenuTheme('" + menuColor + "', '" + menuTheme + "', " + darkMode + ")");
        JSFUtils.saveCookie(COOKIE_NAME_MENU_COLOR, getMenuColor(), COOKIE_PATH, 365, null);
    }

    /**
     * Вика се при смяна на цвета на менюто
     */
    public void setMenuTheme(String menuTheme) {
        this.menuTheme = menuTheme;
        JSFUtils.saveCookie(COOKIE_NAME_MENU_THEME, String.valueOf(getMenuTheme()), COOKIE_PATH, 365, null);
    }

    /**
     * Вика се при смяна на Запълнени/Очертани компоненти
     */
    public void onInputStyleChange() {
        JSFUtils.saveCookie(COOKIE_NAME_INPUT_STYLE, getInputStyle(), COOKIE_PATH, 365, null);
    }

    public String getInputStyleClass() {
        return this.inputStyle.equals("filled") ? "ui-input-filled" : "";
    }

    public void setComponentTheme(String componentTheme) {
        this.componentTheme = componentTheme;
        JSFUtils.saveCookie(COOKIE_NAME_COMP_THEME, String.valueOf(getComponentTheme()), COOKIE_PATH, 365, null);
    }

    public String getInputStyle() {
        return inputStyle;
    }

    public void setInputStyle(String inputStyle) {
        this.inputStyle = inputStyle;
    }

    /**
     * Вика се най-долу в темплейта при зареждането на css файл.
     * @return стринг с името на css файла:
     * 		<li>layout-blue-light</li>
     * 		<li>layout-blue-dark</li>
     * 		<li>layout-yellow-light</li>
     * 		<li>и т.н.</li>
     */
    public String getLayout() {
        return "layout-" + this.menuTheme + (this.darkMode ? "-dark" : "-light");
    }

    /**
     * Вика се в темплейта при layout-wrapper при отваряне на страница
     * @return Връща стринг с класовете:
     * 		<li>layout-menu-light / layout-menu-dark</li>
     * 		<li>layout-horizontal / " "</li>
     */
    public String getLayoutConfig() {
        StringBuilder sb = new StringBuilder();

        String color = getMenuColor();
        sb.append("layout-menu-").append(color);

        if (this.isHorizontalMenu()) {
            sb.append(" layout-horizontal");
        }

        return sb.toString();
    }

    public String getTheme() {
        return this.darkMode ? this.componentTheme + "-dark" : this.componentTheme + "-light";
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        this.menuColor = darkMode ? "dark" : "light";
    }

    public String getComponentTheme() {
        return componentTheme;
    }


    public boolean isHorizontalMenu() {
        return horizontalMenu;
    }

    public void setHorizontalMenu(boolean horizontalMenu) {
        this.horizontalMenu = horizontalMenu;
    }

    public String getMenuTheme() {
        return menuTheme;
    }

    public List<ComponentTheme> getComponentThemes() {
        return componentThemes;
    }

    public void setComponentThemes(List<ComponentTheme> componentThemes) {
        this.componentThemes = componentThemes;
    }

    public String getMenuColor() {
        return menuColor;
    }

    public void setMenuColor(String menuColor) {
        this.menuColor = menuColor;
    }

    public Map<String, List<MenuTheme>> getMenuColors() {
        return menuColors;
    }

    public void setMenuColors(Map<String, List<MenuTheme>> menuColors) {
        this.menuColors = menuColors;
    }

    public String getContainerPosition() {
        return containerPosition;
    }

    public void setContainerPosition(String containerPosition) {
        this.containerPosition = containerPosition;
    }

    public class MenuTheme {

        String name;
        String file;
        String color;

        public MenuTheme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return this.name;
        }

        public String getFile() {
            return this.file;
        }

        public String getColor() {
            return this.color;
        }
    }

    public class ComponentTheme {

        String name;
        String file;
        String color;

        public ComponentTheme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return this.name;
        }

        public String getFile() {
            return this.file;
        }

        public String getColor() {
            return this.color;
        }
    }
}
