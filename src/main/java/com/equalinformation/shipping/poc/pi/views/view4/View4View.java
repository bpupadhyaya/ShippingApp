package com.equalinformation.shipping.poc.pi.views.view4;

import com.equalinformation.shipping.poc.pi.components.MovieDetailsWindow;
import com.equalinformation.shipping.poc.pi.domain.Movie;
import com.equalinformation.shipping.poc.pi.domain.Transaction;
import com.equalinformation.shipping.poc.pi.event.ShippingEvent;
import com.equalinformation.shipping.poc.pi.event.ShippingEventBus;
import com.equalinformation.shipping.poc.pi.views.ShippingUI;
import com.google.common.eventbus.Subscribe;
import com.vaadin.event.LayoutEvents;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.*;
import com.vaadin.ui.components.calendar.CalendarComponentEvents;
import com.vaadin.ui.components.calendar.event.CalendarEvent;
import com.vaadin.ui.components.calendar.event.CalendarEventProvider;
import com.vaadin.ui.components.calendar.handler.BasicEventMoveHandler;
import com.vaadin.ui.components.calendar.handler.BasicEventResizeHandler;
import com.vaadin.ui.themes.ValoTheme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by bpupadhyaya on 12/29/15.
 */
public final class View4View extends CssLayout implements View {

    private Calendar calendar;
    private final Component tray;

    public View4View() {
        setSizeFull();
        addStyleName("schedule");
        ShippingEventBus.register(this);

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);

        tabs.addComponent(buildCalendarView());
        tabs.addComponent(buildCatalogView());

        addComponent(tabs);

        tray = buildTray();
        addComponent(tray);

        injectMovieCoverStyles();
    }

    @Override
    public void detach() {
        super.detach();
        // A new instance of ScheduleView is created every time it's navigated
        // to so we'll need to clean up references to it on detach.
        ShippingEventBus.unregister(this);
    }

    private void injectMovieCoverStyles() {
        // Add all movie cover images as classes to CSSInject
        String styles = "";
        for (Movie m : ShippingUI.getDataProvider().getMovies()) {
            WebBrowser webBrowser = Page.getCurrent().getWebBrowser();

            String bg = "url(VAADIN/themes/" + UI.getCurrent().getTheme()
                    + "/img/event-title-bg.png), url(" + m.getThumbUrl() + ")";

            // IE8 doesn't support multiple background images
            if (webBrowser.isIE() && webBrowser.getBrowserMajorVersion() == 8) {
                bg = "url(" + m.getThumbUrl() + ")";
            }

            styles += ".v-calendar-event-" + m.getId()
                    + " .v-calendar-event-content {background-image:" + bg
                    + ";}";
        }

        Page.getCurrent().getStyles().add(styles);
    }

    private Component buildCalendarView() {
        VerticalLayout calendarLayout = new VerticalLayout();
        calendarLayout.setCaption("Calendar");
        calendarLayout.setMargin(true);

        calendar = new Calendar(new MovieEventProvider());
        calendar.setWidth(100.0f, Unit.PERCENTAGE);
        calendar.setHeight(1000.0f, Unit.PIXELS);

        calendar.setHandler(new CalendarComponentEvents.EventClickHandler() {
            @Override
            public void eventClick(final CalendarComponentEvents.EventClick event) {
                setTrayVisible(false);
                MovieEvent movieEvent = (MovieEvent) event.getCalendarEvent();
                MovieDetailsWindow.open(movieEvent.getMovie(),
                        movieEvent.getStart(), movieEvent.getEnd());
            }
        });
        calendarLayout.addComponent(calendar);

        calendar.setFirstVisibleHourOfDay(11);
        calendar.setLastVisibleHourOfDay(23);

        calendar.setHandler(new BasicEventMoveHandler() {
            @Override
            public void eventMove(final CalendarComponentEvents.MoveEvent event) {
                CalendarEvent calendarEvent = event.getCalendarEvent();
                if (calendarEvent instanceof MovieEvent) {
                    MovieEvent editableEvent = (MovieEvent) calendarEvent;

                    Date newFromTime = event.getNewStart();

                    // Update event dates
                    long length = editableEvent.getEnd().getTime()
                            - editableEvent.getStart().getTime();
                    setDates(editableEvent, newFromTime,
                            new Date(newFromTime.getTime() + length));
                    setTrayVisible(true);
                }
            }

            protected void setDates(final MovieEvent event, final Date start,
                                    final Date end) {
                event.start = start;
                event.end = end;
            }
        });
        calendar.setHandler(new BasicEventResizeHandler() {
            @Override
            public void eventResize(final CalendarComponentEvents.EventResize event) {
                Notification
                        .show("You're not allowed to change the movie duration");
            }
        });

        java.util.Calendar initialView = java.util.Calendar.getInstance();
        initialView.add(java.util.Calendar.DAY_OF_WEEK,
                -initialView.get(java.util.Calendar.DAY_OF_WEEK) + 1);
        calendar.setStartDate(initialView.getTime());

        initialView.add(java.util.Calendar.DAY_OF_WEEK, 6);
        calendar.setEndDate(initialView.getTime());

        return calendarLayout;
    }

    private Component buildCatalogView() {
        CssLayout catalog = new CssLayout();
        catalog.setCaption("Catalog");
        catalog.addStyleName("catalog");

        for (final Movie movie : ShippingUI.getDataProvider().getMovies()) {
            VerticalLayout frame = new VerticalLayout();
            frame.addStyleName("frame");
            frame.setWidthUndefined();

            Image poster = new Image(null, new ExternalResource(
                    movie.getThumbUrl()));
            poster.setWidth(100.0f, Unit.PIXELS);
            poster.setHeight(145.0f, Unit.PIXELS);
            frame.addComponent(poster);

            Label titleLabel = new Label(movie.getTitle());
            titleLabel.setWidth(120.0f, Unit.PIXELS);
            frame.addComponent(titleLabel);

            frame.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
                @Override
                public void layoutClick(final LayoutEvents.LayoutClickEvent event) {
                    if (event.getButton() == MouseEventDetails.MouseButton.LEFT) {
                        MovieDetailsWindow.open(movie, null, null);
                    }
                }
            });
            catalog.addComponent(frame);
        }
        return catalog;
    }

    private Component buildTray() {
        final HorizontalLayout tray = new HorizontalLayout();
        tray.setWidth(100.0f, Unit.PERCENTAGE);
        tray.addStyleName("tray");
        tray.setSpacing(true);
        tray.setMargin(true);

        Label warning = new Label(
                "You have unsaved changes made to the schedule");
        warning.addStyleName("warning");
        warning.addStyleName("icon-attention");
        tray.addComponent(warning);
        tray.setComponentAlignment(warning, Alignment.MIDDLE_LEFT);
        tray.setExpandRatio(warning, 1);

        Button.ClickListener close = new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                setTrayVisible(false);
            }
        };

        Button confirm = new Button("Confirm");
        confirm.addStyleName(ValoTheme.BUTTON_PRIMARY);
        confirm.addClickListener(close);
        tray.addComponent(confirm);
        tray.setComponentAlignment(confirm, Alignment.MIDDLE_LEFT);

        Button discard = new Button("Discard");
        discard.addClickListener(close);
        discard.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                calendar.markAsDirty();
            }
        });
        tray.addComponent(discard);
        tray.setComponentAlignment(discard, Alignment.MIDDLE_LEFT);
        return tray;
    }

    private void setTrayVisible(final boolean visible) {
        final String styleReveal = "v-animate-reveal";
        if (visible) {
            tray.addStyleName(styleReveal);
        } else {
            tray.removeStyleName(styleReveal);
        }
    }

    @Subscribe
    public void browserWindowResized(final ShippingEvent.BrowserResizeEvent event) {
        if (Page.getCurrent().getBrowserWindowWidth() < 800) {
            calendar.setEndDate(calendar.getStartDate());
        }
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
    }

    private class MovieEventProvider implements CalendarEventProvider {

        @Override
        public List<CalendarEvent> getEvents(final Date startDate,
                                             final Date endDate) {
            // Transactions are dynamically fetched from the backend service
            // when needed.
            Collection<Transaction> transactions = ShippingUI
                    .getDataProvider().getTransactionsBetween(startDate,
                            endDate);
            List<CalendarEvent> result = new ArrayList<CalendarEvent>();
            for (Transaction transaction : transactions) {
                Movie movie = ShippingUI.getDataProvider().getMovie(
                        transaction.getMovieId());
                Date end = new Date(transaction.getTime().getTime()
                        + movie.getDuration() * 60 * 1000);
                result.add(new MovieEvent(transaction.getTime(), end, movie));
            }
            return result;
        }
    }

    public final class MovieEvent implements CalendarEvent {

        private Date start;
        private Date end;
        private Movie movie;

        public MovieEvent(final Date start, final Date end, final Movie movie) {
            this.start = start;
            this.end = end;
            this.movie = movie;
        }

        @Override
        public Date getStart() {
            return start;
        }

        @Override
        public Date getEnd() {
            return end;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getStyleName() {
            return String.valueOf(movie.getId());
        }

        @Override
        public boolean isAllDay() {
            return false;
        }

        public Movie getMovie() {
            return movie;
        }

        public void setMovie(final Movie movie) {
            this.movie = movie;
        }

        public void setStart(final Date start) {
            this.start = start;
        }

        public void setEnd(final Date end) {
            this.end = end;
        }

        @Override
        public String getCaption() {
            return movie.getTitle();
        }

    }

}

