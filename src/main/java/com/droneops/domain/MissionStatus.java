package com.droneops.domain;

/** Görev yaşam döngüsü: PLANNED -> ACTIVE -> FINISHED, PLANNED/ACTIVE -> CANCELLED. */
public enum MissionStatus { PLANNED, ACTIVE, FINISHED, CANCELLED }
