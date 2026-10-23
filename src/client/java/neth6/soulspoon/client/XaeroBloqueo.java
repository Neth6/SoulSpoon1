package neth6.soulspoon.client;

/** Estado del bloqueo de minimapa (el servidor decide si los admins lo pueden usar). La integracion con Xaero (mixins de teclas) no esta portada. */
public final class XaeroBloqueo {
    public static volatile boolean adminsLoUsan = false;

    private XaeroBloqueo() {}
}
