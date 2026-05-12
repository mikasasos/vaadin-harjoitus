package com.example.application.views;

import com.example.application.data.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.i18n.I18NProvider;
import com.example.application.security.AuthenticatedUser;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver, LocaleChangeObserver {

    private H1 viewTitle;
    private ComboBox<Locale> languageSelections;
    private Span appName;
    private MenuItem signOutItem;

    private final I18NProvider i18nProvider;

    private AuthenticatedUser authenticatedUser;
    private AccessAnnotationChecker accessChecker;

    public MainLayout(I18NProvider i18nProvider, AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker) {
        this.i18nProvider = i18nProvider;
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;

        WebStorage.getItem("locale", locale -> {
            if (locale != null) {
                UI.getCurrent().setLocale(Locale.forLanguageTag(locale));
            }
        });


        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        appName = new Span(getTranslation("myapp.name"));
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        languageSelections = new ComboBox<>(getTranslation("languageComboBox"));
        languageSelections.setItems(i18nProvider.getProvidedLocales());
        layout.add(languageSelections);
        languageSelections.addValueChangeListener(event -> {
            UI.getCurrent().setLocale(event.getValue());
            WebStorage.setItem("locale", event.getValue().toLanguageTag());
        });


        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

            Avatar avatar = new Avatar(user.getName());


            if (user.getProfilePicture() != null) {
                DownloadHandler imageHandler = DownloadHandler
                        .fromInputStream(event -> new DownloadResponse(
                                new ByteArrayInputStream(user.getProfilePicture()),
                                "profile-pic",
                                "image/jpeg",
                                -1
                        ));
                avatar.setImageHandler(imageHandler);
            }


            avatar.setThemeName("xsmall");
            avatar.getElement().setAttribute("tabindex", "-1");

            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            MenuItem userName = userMenu.addItem("");
            Div div = new Div();
            div.add(avatar);
            div.add(user.getName());
            div.add(new Icon("lumo", "dropdown"));
            div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
            userName.add(div);
            signOutItem = userName.getSubMenu().addItem(getTranslation("signout"), e -> {
                authenticatedUser.logout();
            });

            layout.add(userMenu);
        } else {
            Anchor loginLink = new Anchor("login", getTranslation("login"));
            layout.add(loginLink);
        }

        return layout;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }

    @Override
    public void localeChange(LocaleChangeEvent localeChangeEvent) {
        languageSelections.setLabel(getTranslation("languageComboBox"));
        appName.setText(getTranslation("myapp.name"));
        if(signOutItem != null){
            signOutItem.setText(getTranslation("signout"));
        }
    }
}
