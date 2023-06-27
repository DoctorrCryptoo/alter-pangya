package work.fking.pangya.game.player;

import io.netty.channel.Channel;

public class Player {

    private final int uid;
    private final int connectionId;
    private final Channel channel;

    private final String nickname;

    private final Inventory inventory = new Inventory();
    private final Equipment equipment = new Equipment(this);
    private final CharacterRoster characterRoster = new CharacterRoster();
    private final CaddieRoster caddieRoster = new CaddieRoster();

    private int rank;
    private int experience;

    private int pangBalance = 10000;
    private int cookieBalance;

    public Player(Channel channel, int uid, int connectionId, String nickname) {
        this.channel = channel;
        this.uid = uid;
        this.connectionId = connectionId;
        this.nickname = nickname;
    }

    public Channel channel() {
        return channel;
    }

    public int uid() {
        return uid;
    }

    public int connectionId() {
        return connectionId;
    }

    public String nickname() {
        return nickname;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Equipment equipment() {
        return equipment;
    }

    public CharacterRoster characterRoster() {
        return characterRoster;
    }

    public CaddieRoster caddieRoster() {
        return caddieRoster;
    }

    public int rank() {
        return rank;
    }

    public int experience() {
        return experience;
    }

    public void addExperience(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative experience");
        }
        experience += amount;
    }

    public int pangBalance() {
        return pangBalance;
    }

    public void updatePangBalance(int delta) {
        pangBalance += delta;
    }

    public int cookieBalance() {
        return cookieBalance;
    }

    public void updateCookieBalance(int delta) {
        cookieBalance += delta;
    }

    public Character equippedCharacter() {
        return characterRoster.findByUid(equipment.equippedCharacterUid());
    }

    public Caddie activeCaddie() {
        return caddieRoster.findByUid(equipment.equippedCaddieUid());
    }
}
