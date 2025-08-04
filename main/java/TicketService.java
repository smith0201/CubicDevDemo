package main.java;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Single-class TicketService demo (~230 LOC) with in-memory storage.
 */
public class TicketService {
    private Map<String, Event> events = new HashMap<>();
    private Map<String, Ticket> tickets = new HashMap<>();
    private List<String> auditLog = new ArrayList<>();
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // 1. Instantiate service and seed demo data
        TicketService ticketService = new TicketService();
        ticketService.createEvent("E100", "Rock Concert", 5);
        ticketService.createEvent("E200", "Tech Conference", 3);
        ticketService.printEvents();

        // 2. Purchase tickets
        ticketService.purchase("E100", "alice@example.com");
        ticketService.purchase("E100", "bob@example.com");
        ticketService.purchase("E100", "charlie@example.com");
        ticketService.purchase("E200", "charlie@example.com");
        ticketService.purchase("E200", "charlie1@example.com");

        // 3. List available seats
        ticketService.listAvailableSeats("E100");

        // 4. Cancel a ticket
        String tId = ticketService.purchase("E200", "charlie2@example.com");
        ticketService.cancelTicket(tId);

        // 5. Reporting
        ticketService.salesByEvent();
        ticketService.exportTickets("E100");
        ticketService.exportTickets("E200");
        ticketService.printAuditTrail();
    }

    // CRUD: Create
    public void createEvent(String eventId, String name, int capacity) {
        if (capacity <= 0) {
            log("Invalid capacity for event " + eventId);
            return;
        }
        events.put(eventId, new Event(eventId, name, capacity));
        log("Created event " + eventId + ": " + name + " (cap=" + capacity + ")");
    }

    // CRUD: Read one
    public Event getEvent(String eventId) {
        Event e = events.get(eventId);
        if (e == null) throw new IllegalArgumentException("Event not found: " + eventId);
        return e;
    }

    // CRUD: List all
    public void printEvents() {
        System.out.println("=== Events ===");
        for (Event e : events.values()) System.out.println(e);
        System.out.println();
    }

    // CRUD: Update
    public void updateEvent(String eventId, String newName, Integer newCap) {
        Event e = getEvent(eventId);
        if (newName != null) e.name = newName;
        if (newCap != null && newCap >= e.sold) e.capacity = newCap;
        log("Updated event " + eventId + ": name=" + e.name + ", cap=" + e.capacity);
    }

    // CRUD: Delete
    public void cancelEvent(String eventId) {
        if (!events.containsKey(eventId)) {
            log("Attempt to cancel non-existent event " + eventId);
            return;
        }
        events.remove(eventId);
        log("Canceled event " + eventId);
    }

    // Domain: Purchase a ticket
    public String purchase(String eventId, String purchaser) {
        Event e = getEvent(eventId);
        if (e.sold >= e.capacity) {
            log("Sold out: " + eventId + ", purchaser=" + purchaser);
            System.err.println("Cannot purchase, event sold out: " + eventId);
            return null;
        }
        String ticketId = generateTicketId();
        Ticket t = new Ticket(ticketId, eventId, purchaser);
        tickets.put(ticketId, t);
        e.sold++;
        log("Issued ticket " + ticketId + " for " + purchaser + " to event " + eventId);
        return ticketId;
    }

    // Domain: Cancel a ticket
    public void cancelTicket(String ticketId) {
        Ticket t = tickets.remove(ticketId);
        if (t == null) {
            log("Attempt to cancel non-existent ticket " + ticketId);
            System.err.println("Ticket not found: " + ticketId);
            return;
        }
        Event e = getEvent(t.eventId);
        e.sold--;
        log("Canceled ticket " + ticketId + " for event " + t.eventId);
    }

    // Domain: List available seats
    public void listAvailableSeats(String eventId) {
        Event e = getEvent(eventId);
        int available = e.capacity - e.sold;
        System.out.println("Available seats for " + eventId + ": " + available);
        log("Checked availability for " + eventId + ", available=" + available);
    }

    // Utils: Sales summary
    public void salesByEvent() {
        System.out.println("=== Sales Summary ===");
        for (Event e : events.values()) {
            System.out.println(e.eventId + ": sold=" + e.sold + "/" + e.capacity);
        }
        System.out.println();
        log("Generated sales summary");
    }

    // Utils: Export tickets for an event
    public void exportTickets(String eventId) {
        System.out.println("Exporting tickets for " + eventId + ":");
        for (Ticket t : tickets.values()) {
            if (t.eventId.equals(eventId)) {
                System.out.println(String.join(",", t.ticketId, t.purchaser, DATE_FMT.format(t.issued)));
            }
        }
        System.out.println();
        log("Exported tickets for " + eventId);
    }

    // Utils: Print audit
    public void printAuditTrail() {
        System.out.println("=== Audit Log ===");
        for (String s : auditLog) System.out.println(s);
        System.out.println();
    }

    // Private: Generate unique ticket ID
    private String generateTicketId() {
        return "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Private: Log with timestamp
    private void log(String msg) {
        String time = DATE_FMT.format(new Date());
        auditLog.add(time + " - " + msg);
    }

    // Inner: Event model
    static class Event {
        String eventId;
        String name;
        int capacity;
        int sold = 0;
        Event(String id, String name, int cap) { this.eventId = id; this.name = name; this.capacity = cap; }
        @Override public String toString() { return eventId + ": " + name + " (" + sold + "/" + capacity + ")"; }
    }

    // Inner: Ticket model
    static class Ticket {
        String ticketId;
        String eventId;
        String purchaser;
        Date issued = new Date();
        Ticket(String id, String e, String p) { this.ticketId = id; this.eventId = e; this.purchaser = p; }
    }
}
