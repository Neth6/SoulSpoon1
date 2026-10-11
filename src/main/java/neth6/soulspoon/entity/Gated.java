package neth6.soulspoon.entity;

// Mobs que a veces son invulnerables (Creeper Visceral, Ojo Volador).
public interface Gated {
    boolean gateOpen();

    void setGateOpen(boolean open);
}
