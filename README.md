# Usage

Программа взаимодействует с несколькими публично доступными API
используя методы асинхронного программирования(CompletableFuture)

## Запуск

На вход программе подаётся название какой либо локации, например `Berlin`

Ищутся варианты локаций с помощью метода [1] и показываются пользователю в виде списка

Пользователь выбирает одну локацию

С помощью метода [2] ищется погода в локации

С помощью метода [3] ищутся интересные места в локации, далее для каждого найденного места с помощью 
метода [4] ищутся описания

Всё найденное показывается пользователю

## Публичные API

Методы API:

1. получение локаций с координатами и названиями: https://docs.graphhopper.com/#operation/getGeocode
2. получение погоды по координатам https://openweathermap.org/current
3. получение списка интересных мест по
   координатам: https://opentripmap.io/docs#/Objects%20list/getListOfPlacesByRadius
4. получение описания места по его
   id: https://opentripmap.io/docs#/Object%20properties/getPlaceByXid

## Пример команды запуска

```bash
java -jar AsyncNetworking.main.jar Berlin
```