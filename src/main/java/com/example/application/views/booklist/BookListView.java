package com.example.application.views.booklist;

import com.example.application.data.*;
import com.example.application.services.SampleBookService;
import com.example.application.services.UserService;
import com.example.application.views.MainLayout;
import com.example.application.views.login.LoginView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@Route(value = "/:sampleBookID?/:action?(edit)", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.COLUMNS_SOLID)
@RouteAlias("")
@RolesAllowed({"USER","ADMIN"})
public class BookListView extends Div implements BeforeEnterObserver, LocaleChangeObserver, HasDynamicTitle {

    private final String SAMPLEBOOK_ID = "sampleBookID";
    private final String SAMPLEBOOK_EDIT_ROUTE_TEMPLATE = "/%s/edit";

    private final Grid<SampleBook> grid = new Grid<>(SampleBook.class, false);

    private User currentUser;
    private Upload image;
    private UploadI18N i18n;
    private Image imagePreview;
    private NativeLabel imageLabel;
    private TextField name;
    private TextField author;
    private DatePicker publicationDate;
    private TextField pages;
    private TextField isbn;
    private ComboBox<Status> status;
    private DatePicker dateAdded;

    private Grid.Column<SampleBook> imageColumn;
    private Grid.Column<SampleBook> nameColumn;
    private Grid.Column<SampleBook> authorColumn;
    private Grid.Column<SampleBook> publicationDateColumn;
    private Grid.Column<SampleBook> pagesColumn;
    private Grid.Column<SampleBook> isbnColumn;
    private Grid.Column<SampleBook> statusColumn;
    private Grid.Column<SampleBook> dateAddedColumn;

    /*private TextField description;
    private TextField rating;
    private TextField language;*/

    private final Button cancel = new Button();
    private final Button save = new Button();
    private final Button delete = new Button();


    private final BeanValidationBinder<SampleBook> binder;

    private Filters filters;

    private SampleBook sampleBook;

    private final SampleBookService sampleBookService;
    private final UserService userService;

    public BookListView(SampleBookService sampleBookService, UserService userService, UserRepository userRepository) {
        this.sampleBookService = sampleBookService;
        this.userService = userService;

        addClassNames("book-list-view");

        filters = new Filters(this::refreshGridFromSearch, this.sampleBookService);

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        LitRenderer<SampleBook> imageRenderer = LitRenderer
                .<SampleBook>of("<img style='height: 64px' src=${item.image} />").withProperty("image", item -> {
                    if (item != null && item.getImage() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getImage());
                    } else {
                        return "";
                    }
                });
        imageColumn = grid.addColumn(imageRenderer).setHeader(getTranslation("image")).setWidth("68px").setFlexGrow(0);

        nameColumn = grid.addColumn("name").setHeader(getTranslation("name")).setAutoWidth(true);
        authorColumn = grid.addColumn("author").setHeader(getTranslation("author")).setAutoWidth(true);
        publicationDateColumn = grid.addColumn("publicationDate").setHeader(getTranslation("publicationDate")).setAutoWidth(true);
        pagesColumn = grid.addColumn("pages").setHeader(getTranslation("pages")).setAutoWidth(true);
        isbnColumn = grid.addColumn("isbn").setHeader(getTranslation("isbn")).setAutoWidth(true);
        dateAddedColumn = grid.addColumn("dateAdded").setHeader(getTranslation("dateAdded")).setAutoWidth(true);
        statusColumn = grid.addColumn("status").setHeader(getTranslation("status")).setAutoWidth(true);


        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        grid.setItems(query -> sampleBookService
                .listForUser(username,VaadinSpringDataHelpers.toSpringPageRequest(query),filters)
                .stream());
        grid.addClassName("grid-view");
        grid.getStyle().setColor("brown");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SAMPLEBOOK_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(BookListView.class);
            }
        });

        status.setItems(Status.values());
        status.setItemLabelGenerator(status ->
                getTranslation("status." + status.name().toLowerCase())
        );

        // Configure Form
        binder = new BeanValidationBinder<>(SampleBook.class);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(pages).withConverter(new StringToIntegerConverter(getTranslation("nmbersonlyerror"))).bind("pages");


        binder.bindInstanceFields(this);

        attachImageUpload(image, imagePreview);


        currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.sampleBook == null) {
                    this.sampleBook = new SampleBook();
                }
                binder.writeBean(this.sampleBook);

                sampleBook.setDateAdded(LocalDate.now());
                sampleBook.setUser(currentUser);
                sampleBookService.save(this.sampleBook);

                clearForm();
                refreshGrid();
                Notification.show(getTranslation("data_updated"));
                UI.getCurrent().navigate(BookListView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        getTranslation("update_at_sametime"));
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            } catch (ValidationException validationException) {
                Notification.show(getTranslation("update_fail"));
            }
        });

        delete.addClickListener(e -> {

            try {
                binder.writeBean(this.sampleBook);

                if(this.sampleBook != null && this.sampleBook.getId() != null){
                    sampleBookService.delete(this.sampleBook.getId());

                    grid.getDataProvider().refreshAll();
                    clearForm();
                }
                else {
                    Notification.show(getTranslation("null_id_error"));
                }
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
    @Override
    public String getPageTitle(){
        return getTranslation("pagetitle");
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {

            event.forwardTo(LoginView.class);
            return;
        }

        Optional<Long> sampleBookId = event.getRouteParameters().get(SAMPLEBOOK_ID).map(Long::parseLong);
        if (sampleBookId.isPresent()) {
            Optional<SampleBook> sampleBookFromBackend = sampleBookService.get(sampleBookId.get());
            if (sampleBookFromBackend.isPresent()) {
                populateForm(sampleBookFromBackend.get());
            } else {
                Notification.show(String.format(getTranslation("data_not_found"), sampleBookId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(BookListView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        imageLabel = new NativeLabel(getTranslation("image"));
        imagePreview = new Image();
        imagePreview.setWidth("100%");
        image = new Upload();
        i18n = new UploadI18N();
        i18n.setDropFiles(new UploadI18N.DropFiles().setOne(getTranslation("dropfile")));
        i18n.setAddFiles(new UploadI18N.AddFiles().setOne(getTranslation("uploadfile")));
        image.getStyle().set("box-sizing", "border-box");
        image.getElement().appendChild(imagePreview.getElement());
        image.setI18n(i18n);
        name = new TextField("Name");
        author = new TextField("Author");
        publicationDate = new DatePicker("Publication Date");
        pages = new TextField("Pages");
        isbn = new TextField("Isbn");
        dateAdded = new DatePicker("Date Added");
        dateAdded.setReadOnly(true);
        status = new ComboBox<>("Status");
        formLayout.add(imageLabel, image, name, author, publicationDate, pages, isbn,dateAdded,status);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.ERROR);
        buttonLayout.add(save, cancel, delete);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        VerticalLayout leftLayout = new VerticalLayout();
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        wrapper.add(grid);
        leftLayout.add(filters,wrapper);
        wrapper.setHeightFull();
        splitLayout.addToPrimary(leftLayout);
    }

    public static class Filters extends HorizontalLayout implements Specification<SampleBook>{

        private final TextField name = new TextField();
        private final TextField author = new TextField();
        private final DatePicker startDate = new DatePicker();
        private final DatePicker endDate = new DatePicker();
        private final DatePicker dateAdded = new DatePicker();
        private final ComboBox<Status> status = new ComboBox<>();
        private final Button reset = new Button();
        private final Button search = new Button();



        public Filters(Runnable onSearch,
                       SampleBookService sampleBookService) {

            status.setItems(Status.values());
            status.setItemLabelGenerator(status ->
                    getTranslation("status." + status.name().toLowerCase())
            );

            setWidthFull();

            getStyle().set("flex-wrap", "wrap");
            setSpacing(true);

            addClassName("filter-layout");
            addClassNames(LumoUtility.Padding.Horizontal.LARGE, LumoUtility.Padding.Vertical.MEDIUM,
                    LumoUtility.BoxSizing.BORDER);



            reset.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            reset.addClickListener(e -> {
                name.clear();
                author.clear();
                startDate.clear();
                endDate.clear();
                dateAdded.clear();
                status.clear();
                onSearch.run();
            });

            search.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            search.addClickListener(e -> onSearch.run());

            /* Testasin pushin tällä
            Button testPush = new Button("Test Push");

            testPush.addClickListener(e -> {
                UI ui = UI.getCurrent();

                new Thread(() -> {
                    for (int i = 1; i <= 5; i++) {
                        int value = i;

                        ui.access(() -> {
                            add(new Text("Update: " + value));
                        });

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}
                    }
                }).start();
            });*/

            Div actions = new Div(reset, search);
            //Div actions = new Div(reset, search,testPush);
            actions.addClassName(LumoUtility.Gap.SMALL);
            actions.addClassName("actions");

            add(name, author, createDateRangeFilter(),status ,actions);
        }

        private Component createDateRangeFilter() {

            FlexLayout dateRangeComponent = new FlexLayout(startDate, new Text(" – "), endDate);
            dateRangeComponent.setAlignItems(FlexComponent.Alignment.BASELINE);
            dateRangeComponent.addClassName(LumoUtility.Gap.XSMALL);

            return dateRangeComponent;
        }

        @Override
        public Predicate toPredicate(Root<SampleBook> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            List<Predicate> predicates = new ArrayList<>();

            if (!name.isEmpty()) {
                String lowerCaseFilter = name.getValue().toLowerCase();
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                        "%" + lowerCaseFilter + "%"));
            }
            if (!author.isEmpty()) {
                String lowerCaseFilter = author.getValue().toLowerCase();
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("author")),
                        "%" + lowerCaseFilter + "%"));
            }
            if (startDate.getValue() != null) {
                String databaseColumn = "publicationDate";
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(databaseColumn),
                        criteriaBuilder.literal(startDate.getValue())));
            }

            if(!status.isEmpty()){
                predicates.add(
                        criteriaBuilder.equal(root.get("status"), status.getValue())
                );
            }
            if (endDate.getValue() != null) {
                String databaseColumn = "publicationDate";
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.literal(endDate.getValue()),
                        root.get(databaseColumn)));
            }
            //OR
            if(!name.isEmpty() || !author.isEmpty()){
                String namefilter = name.getValue().toLowerCase();
                String authorfilter = author.getValue().toLowerCase();

                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + namefilter + "%");
                Predicate authorPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("author")),"%" + authorfilter + "%");

                predicates.add(criteriaBuilder.or(namePredicate,authorPredicate));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }

        private String ignoreCharacters(String characters, String in) {
            String result = in;
            for (int i = 0; i < characters.length(); i++) {
                result = result.replace("" + characters.charAt(i), "");
            }
            return result;
        }

        private Expression<String> ignoreCharacters(String characters, CriteriaBuilder criteriaBuilder,
                                                    Expression<String> inExpression) {
            Expression<String> expression = inExpression;
            for (int i = 0; i < characters.length(); i++) {
                expression = criteriaBuilder.function("replace", String.class, expression,
                        criteriaBuilder.literal(characters.charAt(i)), criteriaBuilder.literal(""));
            }
            return expression;
        }
        public void applyTranslation() {
            name.setLabel(getTranslation("name"));
            author.setLabel(getTranslation("author"));
            status.setLabel(getTranslation("status"));
            startDate.setLabel(getTranslation("startDate"));
            endDate.setLabel(getTranslation("endDate"));
            dateAdded.setLabel(getTranslation("dateAdded"));
            reset.setText(getTranslation("reset"));
            search.setText(getTranslation("search"));
            startDate.setPlaceholder(getTranslation("from"));
            endDate.setPlaceholder(getTranslation("to"));
            name.setPlaceholder(getTranslation("bookname"));
            // For screen readers
            startDate.setAriaLabel(getTranslation("from"));
            endDate.setAriaLabel(getTranslation("to"));
            status.setItemLabelGenerator(status ->
                    getTranslation("status." + status.name().toLowerCase())
            );
        }

    }

    private void refreshGridFromSearch() {
        grid.getDataProvider().refreshAll();
    }


    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.sampleBook == null) {
                this.sampleBook = new SampleBook();
            }
            this.sampleBook.setImage(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(SampleBook value) {
        this.sampleBook = value;
        binder.readBean(this.sampleBook);
        this.imagePreview.setVisible(value != null);
        if (value == null || value.getImage() == null) {
            this.image.clearFileList();
            this.imagePreview.setSrc("");
        } else {
            this.imagePreview.setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getImage()));
        }

    }
    @Override
    public void localeChange(LocaleChangeEvent localeChangeEvent) {
        filters.applyTranslation();


        cancel.setText(getTranslation("cancel"));
        save.setText(getTranslation("save"));
        delete.setText(getTranslation("delete"));

        name.setLabel(getTranslation("name"));
        author.setLabel(getTranslation("author"));
        pages.setLabel(getTranslation("pages"));
        isbn.setLabel(getTranslation("isbn"));
        status.setLabel(getTranslation("status"));
        dateAdded.setLabel(getTranslation("dateAdded"));
        publicationDate.setLabel(getTranslation("publicationDate"));
        imageLabel.setText(getTranslation("image"));

        imageColumn.setHeader(getTranslation("image"));
        nameColumn.setHeader(getTranslation("name"));
        authorColumn.setHeader(getTranslation("author"));
        pagesColumn.setHeader(getTranslation("pages"));
        isbnColumn.setHeader(getTranslation("isbn"));
        statusColumn.setHeader(getTranslation("status"));
        dateAddedColumn.setHeader(getTranslation("dateAdded"));
        publicationDateColumn.setHeader(getTranslation("publicationDate"));

        i18n.setDropFiles(new UploadI18N.DropFiles().setOne(getTranslation("dropfile")));
        i18n.setAddFiles(new UploadI18N.AddFiles().setOne(getTranslation("uploadfile")));
        image.setI18n(i18n);

        status.setItemLabelGenerator(item ->
                getTranslation("status." + item.name().toLowerCase()));

        UI.getCurrent().navigate(BookListView.class);
    }
}
