package com.example.minidrive.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Некорректные данные запроса"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "Новый пароль не должен совпадать со старым"),
    INVALID_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "Текущий пароль введён неверно"),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "Неверный код подтверждения"),
    NO_CODE_FOUND(HttpStatus.BAD_REQUEST, "Код не найден или истёк"),
    EMAIL_VERIFIED(HttpStatus.BAD_REQUEST, "Этот email уже подтверждён"),
    TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, "Этот токен уже был использован"),
    NOT_A_FOLDER(HttpStatus.BAD_REQUEST, "Родительский элемент должен быть папкой"),
    QUOTA_EXCEEDED(HttpStatus.BAD_REQUEST, "Рамер файла превышает количество доступного пространства"),
    CHUNK_SIZE_TOO_SMALL(HttpStatus.BAD_REQUEST, "Запрошеный размер чанка слишком мал"),
    CHUNK_SIZE_TOO_LARGE(HttpStatus.BAD_REQUEST, "Запрошеный размер чанка слишком велик"),
    INCOMPLETE_UPLOAD(HttpStatus.BAD_REQUEST, "Загружены не все чанки."),
    UPLOAD_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "Сессия загрузки не найдена или истекла"),
    STORAGE_IN_USE(HttpStatus.BAD_REQUEST, "Нельзя удалить хранилище, в котором есть файлы"),

    // 401 Unauthorized
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh-токен истёк"),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Password-reset токен истёк"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh-токен недействителен"),
    TOKEN_STOLEN(HttpStatus.UNAUTHORIZED, "Обнаружена попытка повторного использования токена"),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Недостаточно прав"),
    INVALID_TOKEN_PURPOSE(HttpStatus.FORBIDDEN, "Недействительный токен для этого действия"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Пользователь не найден"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Файл не найден"),
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Папка не найдена"),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Группа не найдена"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Запрошенный ресурс не найден"),
    STORAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Хранилище не найдено"),

    // 409 Conflict
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Этот email уже зарегистрирован"),
    USERNAME_EXISTS(HttpStatus.CONFLICT, "Этот username уже зарегистрирован"),
    GROUP_EXISTS(HttpStatus.CONFLICT, "Такая группа уже существует"),
    STORAGE_EXISTS(HttpStatus.CONFLICT, "Хранилище с таким именем уже существует"),
    FILENAME_IN_FOLDER_EXISTS(HttpStatus.CONFLICT, "Файл с таким именем уже существует в этой папке"),

    // 429 Too Many Requests
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Слишком много неверных попыток ввести код. Запросите новый"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Слишком много запросов. Попробуйте позже"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Превышен лимит запросов за определённое время"),
    TOO_MANY_ACTIVE_UPLOADS(HttpStatus.TOO_MANY_REQUESTS, "Достигнут лимит параллельных загрузок"),

    // 500 Internal Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера"),
    CHUNK_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка записи чанка"),
    FILE_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка записи файла");


    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
