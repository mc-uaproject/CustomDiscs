package me.Navoei.customdiscsplugin.language;

import org.bukkit.configuration.file.YamlConfiguration;

public enum Lang {
    PREFIX("prefix", "&8[&6CustomDiscs&8]&r"),
    PLAYER_NOT_FOUND("player-not-found", "&rГравець не знайдений!"),
    TOKEN_GRANTED_OTHER("token-granted-other", "&aТокен видано гравцю &7%player%&a!"),
    TOKEN_GRANTED("token-granted", "&aТокен видано!"),
    NO_TOKEN("no-token", "&rУ вашому інвентарі немає токена."),
    NO_PERMISSION("no-permission", "&rУ вас немає дозволу на виконання цієї команди."),
    INVALID_FILENAME("invalid-filename", "&rНевірна назва файлу!"),
    INVALID_FORMAT("invalid-format", "&rФайл має бути у форматі wav, flac або mp3!"),
    FILE_NOT_FOUND("file-not-found", "&rФайл не знайдено!"),
    INVALID_ARGUMENTS("invalid-arguments", "&rНедостатньо аргументів. &7(&a%command_syntax%&7)"),
    NOT_HOLDING_DISC("not-holding-disc", "&rВи повинні тримати диск у основній руці."),
    CREATE_FILENAME("create-filename", "&7Назва вашого файлу: &a\"%filename%\"."),
    CREATE_CUSTOM_NAME("create-custom-name", "&7Ваша власна назва: &a\"%custom_name%\"."),
    DOWNLOADING_FILE("downloading-file", "&7Завантаження файлу..."),
    FILE_TOO_LARGE("file-too-large", "&rФайл занадто великий (більше %max_download_size%МБ)."),
    SUCCESSFUL_DOWNLOAD("successful-download", "&aФайл успішно завантажено до &7%file_path%&a."),
    CREATE_DISC("create-disc", "&aСтворіть диск за допомогою &7/cd create назва_файлу.розширення \"Опис\"&a."),
    DOWNLOAD_ERROR("download-error", "&rПід час завантаження сталася помилка."),
    NOW_PLAYING("now-playing", "&6Зараз грає: %song_name%"),
    DISC_CONVERTED("disc-converted", "&aДиск конвертовано у новий формат! &fЦе пов’язано зі змінами у нових версіях Minecraft, які ввели &7JukeboxPlayableComponent&f."),
    INVALID_RANGE("invalid-range", "&rВиберіть значення від 1 до %range_value%"),
    CREATE_CUSTOM_RANGE("create-custom-range", "&7Ваш діапазон встановлено на: &a\"%custom_range%\"."),
    NO_DISCS_IN_INVENTORY("no-discs", "&cУ вашому інвентарі немає кастомних дисків!"),
    NOW_PLAYING_PORTABLE("now-playing-portable", "&6Грає: %song_name%"),
    STOPPED_PLAYING("stopped-playing", "&cВідтворення призупинено!"),
    TOKEN_FOUND("token-found", "&aТокен знайдено! Обробка вашого запиту..."),
    CREATING_DISC("creating-disc", "&7Створення диску з назвою: &f%song_name%"),
    SUPPORTED_FORMATS("supported-formats", "&7Підтримувані формати: &fwav, mp3, flac"),
    URL_INFO("url-info", "&7URL: &f%url%"),
    FILENAME_INFO("filename-info", "&7Ім'я файлу: &f%filename%"),
    FILE_SIZE_INFO("file-size-info", "&7Розмір файлу: &f%size% МБ"),
    CREATING_DISC_ITEM("creating-disc-item", "&aСтворення елементу диску..."),
    INVENTORY_FULL("inventory-full", "&cІнвентар заповнений! &aДиск викинуто біля вас."),
    DISC_ADDED("disc-added", "&aДиск додано до вашого інвентарю!"),
    TOKEN_USED("token-used", "&aТокен використано."),
    DISC_CREATED("disc-created", "&aКастомний диск успішно створено!"),
    ERROR_DETAILS("error-details", "&cДеталі помилки: &f%error%"),
    CHECK_URL("check-url", "&cПеревірте, будь ласка, що URL-адреса правильна і файл доступний."),
    TOKEN_REQUIRED("token-required", "&cВам потрібен токен, щоб створити кастомний диск. Запитайте адміністратора.");

    private final String path;
    private final String def;
    private static YamlConfiguration LANG;

    /**
     * Lang enum constructor.
     *
     * @param path  The string path.
     * @param start The default string.
     */
    Lang(String path, String start) {
        this.path = path;
        this.def = start;
    }

    /**
     * Set the {@code YamlConfiguration} to use.
     *
     * @param config The config to set.
     */
    public static void setFile(YamlConfiguration config) {
        LANG = config;
    }

    @Override
    public String toString() {
        if (this == PREFIX)
            return LANG.getString(this.path, def) + " ";
        return LANG.getString(this.path, def);
    }

    public String replace(String placeholder, String replacement) {
        return this.toString().replace(placeholder, replacement);
    }

    /**
     * Get the default value of the path.
     *
     * @return The default value of the path.
     */
    public String getDefault() {
        return this.def;
    }

    /**
     * Get the path to the string.
     *
     * @return The path to the string.
     */
    public String getPath() {
        return this.path;
    }

    //Component textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderAPI.setPlaceholders(player, Lang.PREFIX + Lang.COMBAT.toString()));
    //player.sendMessage(textComponent);
}
