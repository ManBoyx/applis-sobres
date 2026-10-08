package fr.manboyx.notes.logique;

/** Une note telle qu'elle apparaît dans la liste. */
public final class Note {
    public final long id;
    /** La première ligne non vide. */
    public final String titre;
    /** La suite, sur une ligne. */
    public final String extrait;
    public final long modifiee;

    Note(long id, String titre, String extrait, long modifiee) {
        this.id = id;
        this.titre = titre;
        this.extrait = extrait;
        this.modifiee = modifiee;
    }
}
