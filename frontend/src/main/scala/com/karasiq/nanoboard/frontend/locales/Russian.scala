package com.karasiq.nanoboard.frontend.locales

import com.karasiq.nanoboard.api.NanoboardContainer

object Russian extends BoardLocale {
  def nanoboard = "Наноборда"
  def generateContainer = "Создать контейнер"
  def cancel = "Отмена"
  def dataContainer = "Исходный файл"
  def dequeue = "Из очереди"
  def insertImage = "Изображение"
  def recentPostsFrom(post: Int) = s"Недавние сообщения, начиная с $post"
  def categories = "Категории"
  def pendingPosts = "Ожидающие отправления сообщения"
  def containerGeneration = "Генерация контейнера"
  def delete = "Удалить"
  def imageFormat = "Формат изображения"
  def enqueue = "В очередь"
  def recentPosts = "Недавние сообщения"
  def reply = "Ответить"
  def settings = "Настройки"
  def places = "Треды с контейнерами"
  def imageSize = "Размер изображения"
  def submit = "Отправить"
  def randomPosts = "Случайные сообщения"
  def imageQuality = "Качество изображения"
  def deleteConfirmation(hash: String) = s"Вы уверены, что хотите навсегда удалить сообщение #$hash?"
  def writeYourMessage = "Введите сообщение"
  def bytes = "байт"
  def style = "Стиль оформления"
  def fromTo(from: Int, to: Int) = s"С $from по $to"
  def embeddedImage = "Встроенное изображение"
  def fileNotSelected = "Файл не выбран"
  def preferences = "Опции"
  def control = "Управление"
  def offset = "Начиная с"
  def count = "Количество"
  def batchDelete = "Массовое удаление"
  def batchDeleteConfirmation(count: Int) = s"Вы уверены, что хотите навсегда удалить $count сообщений?"
  def batchDeleteSuccess(count: Int) = s"$count сообщений успешно удалено"
  def clearDeleted = "Очистка удалённых сообщений"
  def clearDeletedConfirmation = "Очистить кэш удалённых сообщений?"
  def clearDeletedSuccess(count: Int) = s"$count удалённых сообщений очищенно"
  def clearDeletedError = "Ошибка очистки кэша удалённых сообщений"
  def postingError = "Ошибка отправки сообщения"
  def updateError = "Ошибка обновления"
  def containerGenerationError = "Ошибка создания контейнера"
  def attachmentGenerationError = "Ошибка вставки изображения"
  def batchDeleteError = "Ошибка массового удаления"
  def settingsUpdateError = "Ошибка применения настроек"
  def containers = "Принятые контейнеры"
  def container(c: NanoboardContainer) = s"№${c.id}, ${c.posts} сообщений"
}
