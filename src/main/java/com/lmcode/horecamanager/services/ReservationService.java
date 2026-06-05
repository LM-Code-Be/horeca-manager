package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.models.Reservation;
import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.ReservationRepository;
import com.lmcode.horecamanager.repositories.TableRepository;

import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final TableRepository tableRepository = new TableRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final ValidationService validationService = new ValidationService();

    public int save(Reservation reservation) {
        validationService.validateReservation(
                reservation.customerName(),
                reservation.phone(),
                reservation.email(),
                reservation.reservationDate(),
                reservation.guestsCount()
        );
        int id = reservationRepository.save(reservation);
        syncTableStatus(reservation);
        return id;
    }

    public void update(Reservation reservation) {
        validationService.validateReservation(
                reservation.customerName(),
                reservation.phone(),
                reservation.email(),
                reservation.reservationDate(),
                reservation.guestsCount()
        );
        reservationRepository.update(reservation);
        syncTableStatus(reservation);
    }

    public void confirm(int id) {
        reservationRepository.updateStatus(id, "CONFIRMEE");
        Reservation reservation = reservationRepository.findById(id);
        syncTableStatus(reservation);
    }

    public void arrived(int id) {
        reservationRepository.updateStatus(id, "ARRIVEE");
        Reservation reservation = reservationRepository.findById(id);
        if (reservation != null && reservation.tableId() != null) {
            tableRepository.updateStatus(reservation.tableId(), "OCCUPEE");
        }
    }

    public void cancel(int id) {
        Reservation reservation = reservationRepository.findById(id);
        reservationRepository.updateStatus(id, "ANNULEE");
        if (reservation != null && reservation.tableId() != null && orderRepository.findOpenByTableId(reservation.tableId()) == null) {
            tableRepository.updateStatus(reservation.tableId(), "LIBRE");
        }
    }

    private void syncTableStatus(Reservation reservation) {
        if (reservation == null || reservation.tableId() == null || !reservation.reservationDate().equals(LocalDate.now())) {
            return;
        }
        if ("CONFIRMEE".equals(reservation.status()) || "EN_ATTENTE".equals(reservation.status())) {
            if (orderRepository.findOpenByTableId(reservation.tableId()) == null) {
                tableRepository.updateStatus(reservation.tableId(), "RESERVEE");
            }
        }
    }
}
