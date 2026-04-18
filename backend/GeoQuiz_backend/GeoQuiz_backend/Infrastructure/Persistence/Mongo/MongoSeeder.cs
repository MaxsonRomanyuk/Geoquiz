using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using MongoDB.Driver;
using System.Globalization;
using System.Text;

namespace GeoQuiz_backend.Infrastructure.Persistence.Mongo
{
    public class MongoSeeder
    {
        private readonly MongoContext _context;
        private readonly IMongoCollection<Country> _countries;
        private readonly IMongoCollection<Question> _questions;

        public MongoSeeder(MongoContext context)
        {
            _context = context;
            _countries = context.GetCollection<Country>("countries");
            _questions = context.GetCollection<Question>("questions");
        }

        public async Task SeedCountriesAsync()
        {
            if (await _countries.CountDocumentsAsync(_ => true) > 0)
                return;

            var rawCountries = new[]
            {
                new[] { "Австралия", "Australia", "oceania", "Канберра", "Canberra", "images/flags/australia.png", "images/outline/australia.png" },
                new[] { "Австрия", "Austria", "europe", "Вена", "Vienna", "images/flags/austria.png", "images/outline/austria.png" },
                new[] { "Азербайджан", "Azerbaijan", "asia", "Баку", "Baku", "images/flags/azerbaijan.png", "images/outline/azerbaijan.png" },
                new[] { "Албания", "Albania", "europe", "Тирана", "Tirana", "images/flags/albania.png", "images/outline/albania.png" },
                new[] { "Алжир", "Algeria", "africa", "Алжир", "Algiers", "images/flags/algeria.png", "images/outline/algeria.png" },
                new[] { "Ангола", "Angola", "africa", "Луанда", "Luanda", "images/flags/angola.png", "images/outline/angola.png" },
                new[] { "Андорра", "Andorra", "europe", "Андорра-ла-Велья", "Andorra la Vella", "images/flags/andorra.png", "images/outline/andorra.png" },
                new[] { "Антигуа и Барбуда", "Antigua and Barbuda", "america", "Сент-Джонс", "St. John's", "images/flags/antigua_barbuda.png", "images/outline/antigua_barbuda.png" },
                new[] { "Аргентина", "Argentina", "america", "Буэнос-Айрес", "Buenos Aires", "images/flags/argentina.png", "images/outline/argentina.png" },
                new[] { "Армения", "Armenia", "asia", "Ереван", "Yerevan", "images/flags/armenia.png", "images/outline/armenia.png" },
                new[] { "Афганистан", "Afghanistan", "asia", "Кабул", "Kabul", "images/flags/afghanistan.png", "images/outline/afghanistan.png" },
                new[] { "Багамские Острова", "Bahamas", "america", "Нассау", "Nassau", "images/flags/bahamas.png", "images/outline/bahamas.png" },
                new[] { "Бангладеш", "Bangladesh", "asia", "Дакка", "Dhaka", "images/flags/bangladesh.png", "images/outline/bangladesh.png" },
                new[] { "Барбадос", "Barbados", "america", "Бриджтаун", "Bridgetown", "images/flags/barbados.png", "images/outline/barbados.png" },
                new[] { "Бахрейн", "Bahrain", "asia", "Манама", "Manama", "images/flags/bahrain.png", "images/outline/bahrain.png" },
                new[] { "Белиз", "Belize", "america", "Бельмопан", "Belmopan", "images/flags/belize.png", "images/outline/belize.png" },
                new[] { "Беларусь", "Belarus", "europe", "Минск", "Minsk", "images/flags/belarus.png", "images/outline/belarus.png" },
                new[] { "Бельгия", "Belgium", "europe", "Брюссель", "Brussels", "images/flags/belgium.png", "images/outline/belgium.png" },
                new[] { "Бенин", "Benin", "africa", "Порто-Ново", "Porto-Novo", "images/flags/benin.png", "images/outline/benin.png" },
                new[] { "Болгария", "Bulgaria", "europe", "София", "Sofia", "images/flags/bulgaria.png", "images/outline/bulgaria.png" },
                new[] { "Боливия", "Bolivia", "america", "Сукре", "Sucre", "images/flags/bolivia.png", "images/outline/bolivia.png" },
                new[] { "Босния и Герцеговина", "Bosnia and Herzegovina", "europe", "Сараево", "Sarajevo", "images/flags/bosnia_herzegovina.png", "images/outline/bosnia_herzegovina.png" },
                new[] { "Ботсвана", "Botswana", "africa", "Габороне", "Gaborone", "images/flags/botswana.png", "images/outline/botswana.png" },
                new[] { "Бразилия", "Brazil", "america", "Бразилиа", "Brasília", "images/flags/brazil.png", "images/outline/brazil.png" },
                new[] { "Бруней", "Brunei", "asia", "Бандар-Сери-Бегаван", "Bandar Seri Begawan", "images/flags/brunei.png", "images/outline/brunei.png" },
                new[] { "Буркина-Фасо", "Burkina Faso", "africa", "Уагадугу", "Ouagadougou", "images/flags/burkina_faso.png", "images/outline/burkina_faso.png" },
                new[] { "Бурунди", "Burundi", "africa", "Гитега", "Gitega", "images/flags/burundi.png", "images/outline/burundi.png" },
                new[] { "Бутан", "Bhutan", "asia", "Тхимпху", "Thimphu", "images/flags/bhutan.png", "images/outline/bhutan.png" },
                new[] { "Вануату", "Vanuatu", "oceania", "Порт-Вила", "Port Vila", "images/flags/vanuatu.png", "images/outline/vanuatu.png" },
                new[] { "Ватикан", "Vatican City", "europe", "Ватикан", "Vatican City", "images/flags/vatican.png", "images/outline/vatican.png" },
                new[] { "Великобритания", "United Kingdom", "europe", "Лондон", "London", "images/flags/uk.png", "images/outline/uk.png" },
                new[] { "Венгрия", "Hungary", "europe", "Будапешт", "Budapest", "images/flags/hungary.png", "images/outline/hungary.png" },
                new[] { "Венесуэла", "Venezuela", "america", "Каракас", "Caracas", "images/flags/venezuela.png", "images/outline/venezuela.png" },
                new[] { "Восточный Тимор", "Timor-Leste", "asia", "Дили", "Dili", "images/flags/timor_leste.png", "images/outline/timor_leste.png" },
                new[] { "Вьетнам", "Vietnam", "asia", "Ханой", "Hanoi", "images/flags/vietnam.png", "images/outline/vietnam.png" },
                new[] { "Габон", "Gabon", "africa", "Либревиль", "Libreville", "images/flags/gabon.png", "images/outline/gabon.png" },
                new[] { "Гаити", "Haiti", "america", "Порт-о-Пренс", "Port-au-Prince", "images/flags/haiti.png", "images/outline/haiti.png" },
                new[] { "Гайана", "Guyana", "america", "Джорджтаун", "Georgetown", "images/flags/guyana.png", "images/outline/guyana.png" },
                new[] { "Гамбия", "Gambia", "africa", "Банжул", "Banjul", "images/flags/gambia.png", "images/outline/gambia.png" },
                new[] { "Гана", "Ghana", "africa", "Аккра", "Accra", "images/flags/ghana.png", "images/outline/ghana.png" },
                new[] { "Гватемала", "Guatemala", "america", "Гватемала", "Guatemala City", "images/flags/guatemala.png", "images/outline/guatemala.png" },
                new[] { "Гвинея", "Guinea", "africa", "Конакри", "Conakry", "images/flags/guinea.png", "images/outline/guinea.png" },
                new[] { "Гвинея-Бисау", "Guinea-Bissau", "africa", "Бисау", "Bissau", "images/flags/guinea_bissau.png", "images/outline/guinea_bissau.png" },
                new[] { "Германия", "Germany", "europe", "Берлин", "Berlin", "images/flags/germany.png", "images/outline/germany.png" },
                new[] { "Гондурас", "Honduras", "america", "Тегусигальпа", "Tegucigalpa", "images/flags/honduras.png", "images/outline/honduras.png" },
                new[] { "Гренада", "Grenada", "america", "Сент-Джорджес", "St. George's", "images/flags/grenada.png", "images/outline/grenada.png" },
                new[] { "Греция", "Greece", "europe", "Афины", "Athens", "images/flags/greece.png", "images/outline/greece.png" },
                new[] { "Грузия", "Georgia", "asia", "Тбилиси", "Tbilisi", "images/flags/georgia.png", "images/outline/georgia.png" },
                new[] { "Дания", "Denmark", "europe", "Копенгаген", "Copenhagen", "images/flags/denmark.png", "images/outline/denmark.png" },
                new[] { "Джибути", "Djibouti", "africa", "Джибути", "Djibouti", "images/flags/djibouti.png", "images/outline/djibouti.png" },
                new[] { "Доминика", "Dominica", "america", "Розо", "Roseau", "images/flags/dominica.png", "images/outline/dominica.png" },
                new[] { "Доминиканская Республика", "Dominican Republic", "america", "Санто-Доминго", "Santo Domingo", "images/flags/dominican_republic.png", "images/outline/dominican_republic.png" },
                new[] { "Египет", "Egypt", "africa", "Каир", "Cairo", "images/flags/egypt.png", "images/outline/egypt.png" },
                new[] { "Замбия", "Zambia", "africa", "Лусака", "Lusaka", "images/flags/zambia.png", "images/outline/zambia.png" },
                new[] { "Зимбабве", "Zimbabwe", "africa", "Хараре", "Harare", "images/flags/zimbabwe.png", "images/outline/zimbabwe.png" },
                new[] { "Израиль", "Israel", "asia", "Иерусалим", "Jerusalem", "images/flags/israel.png", "images/outline/israel.png" },
                new[] { "Индия", "India", "asia", "Нью-Дели", "New Delhi", "images/flags/india.png", "images/outline/india.png" },
                new[] { "Индонезия", "Indonesia", "asia", "Джакарта", "Jakarta", "images/flags/indonesia.png", "images/outline/indonesia.png" },
                new[] { "Иордания", "Jordan", "asia", "Амман", "Amman", "images/flags/jordan.png", "images/outline/jordan.png" },
                new[] { "Ирак", "Iraq", "asia", "Багдад", "Baghdad", "images/flags/iraq.png", "images/outline/iraq.png" },
                new[] { "Иран", "Iran", "asia", "Тегеран", "Tehran", "images/flags/iran.png", "images/outline/iran.png" },
                new[] { "Ирландия", "Ireland", "europe", "Дублин", "Dublin", "images/flags/ireland.png", "images/outline/ireland.png" },
                new[] { "Исландия", "Iceland", "europe", "Рейкьявик", "Reykjavík", "images/flags/iceland.png", "images/outline/iceland.png" },
                new[] { "Испания", "Spain", "europe", "Мадрид", "Madrid", "images/flags/spain.png", "images/outline/spain.png" },
                new[] { "Италия", "Italy", "europe", "Рим", "Rome", "images/flags/italy.png", "images/outline/italy.png" },
                new[] { "Йемен", "Yemen", "asia", "Сана", "Sana'a", "images/flags/yemen.png", "images/outline/yemen.png" },
                new[] { "Кабо-Верде", "Cape Verde", "africa", "Прая", "Praia", "images/flags/cape_verde.png", "images/outline/cape_verde.png" },
                new[] { "Казахстан", "Kazakhstan", "asia", "Нур-Султан", "Nur-Sultan", "images/flags/kazakhstan.png", "images/outline/kazakhstan.png" },
                new[] { "Камбоджа", "Cambodia", "asia", "Пномпень", "Phnom Penh", "images/flags/cambodia.png", "images/outline/cambodia.png" },
                new[] { "Камерун", "Cameroon", "africa", "Яунде", "Yaoundé", "images/flags/cameroon.png", "images/outline/cameroon.png" },
                new[] { "Канада", "Canada", "america", "Оттава", "Ottawa", "images/flags/canada.png", "images/outline/canada.png" },
                new[] { "Катар", "Qatar", "asia", "Доха", "Doha", "images/flags/qatar.png", "images/outline/qatar.png" },
                new[] { "Кения", "Kenya", "africa", "Найроби", "Nairobi", "images/flags/kenya.png", "images/outline/kenya.png" },
                new[] { "Кипр", "Cyprus", "asia", "Никосия", "Nicosia", "images/flags/cyprus.png", "images/outline/cyprus.png" },
                new[] { "Киргизия", "Kyrgyzstan", "asia", "Бишкек", "Bishkek", "images/flags/kyrgyzstan.png", "images/outline/kyrgyzstan.png" },
                new[] { "Кирибати", "Kiribati", "oceania", "Южная Тарава", "South Tarawa", "images/flags/kiribati.png", "images/outline/kiribati.png" },
                new[] { "Китай", "China", "asia", "Пекин", "Beijing", "images/flags/china.png", "images/outline/china.png" },
                new[] { "Колумбия", "Colombia", "america", "Богота", "Bogotá", "images/flags/colombia.png", "images/outline/colombia.png" },
                new[] { "Коморы", "Comoros", "africa", "Морони", "Moroni", "images/flags/comoros.png", "images/outline/comoros.png" },
                new[] { "Конго", "Congo", "africa", "Браззавиль", "Brazzaville", "images/flags/congo.png", "images/outline/congo.png" },
                new[] { "Демократическая Республика Конго", "DR Congo", "africa", "Киншаса", "Kinshasa", "images/flags/dr_congo.png", "images/outline/dr_congo.png" },
                new[] { "Корейская Народно-Демократическая Республика", "North Korea", "asia", "Пхеньян", "Pyongyang", "images/flags/north_korea.png", "images/outline/north_korea.png" },
                new[] { "Республика Корея", "South Korea", "asia", "Сеул", "Seoul", "images/flags/south_korea.png", "images/outline/south_korea.png" },
                new[] { "Коста-Рика", "Costa Rica", "america", "Сан-Хосе", "San José", "images/flags/costa_rica.png", "images/outline/costa_rica.png" },
                new[] { "Кот-д'Ивуар", "Ivory Coast", "africa", "Ямусукро", "Yamoussoukro", "images/flags/ivory_coast.png", "images/outline/ivory_coast.png" },
                new[] { "Куба", "Cuba", "america", "Гавана", "Havana", "images/flags/cuba.png", "images/outline/cuba.png" },
                new[] { "Кувейт", "Kuwait", "asia", "Эль-Кувейт", "Kuwait City", "images/flags/kuwait.png", "images/outline/kuwait.png" },
                new[] { "Лаос", "Laos", "asia", "Вьентьян", "Vientiane", "images/flags/laos.png", "images/outline/laos.png" },
                new[] { "Латвия", "Latvia", "europe", "Рига", "Riga", "images/flags/latvia.png", "images/outline/latvia.png" },
                new[] { "Лесото", "Lesotho", "africa", "Масеру", "Maseru", "images/flags/lesotho.png", "images/outline/lesotho.png" },
                new[] { "Ливан", "Lebanon", "asia", "Бейрут", "Beirut", "images/flags/lebanon.png", "images/outline/lebanon.png" },
                new[] { "Ливия", "Libya", "africa", "Триполи", "Tripoli", "images/flags/libya.png", "images/outline/libya.png" },
                new[] { "Либерия", "Liberia", "africa", "Монровия", "Monrovia", "images/flags/liberia.png", "images/outline/liberia.png" },
                new[] { "Лихтенштейн", "Liechtenstein", "europe", "Вадуц", "Vaduz", "images/flags/liechtenstein.png", "images/outline/liechtenstein.png" },
                new[] { "Литва", "Lithuania", "europe", "Вильнюс", "Vilnius", "images/flags/lithuania.png", "images/outline/lithuania.png" },
                new[] { "Люксембург", "Luxembourg", "europe", "Люксембург", "Luxembourg", "images/flags/luxembourg.png", "images/outline/luxembourg.png" },
                new[] { "Маврикий", "Mauritius", "africa", "Порт-Луи", "Port Louis", "images/flags/mauritius.png", "images/outline/mauritius.png" },
                new[] { "Мавритания", "Mauritania", "africa", "Нуакшот", "Nouakchott", "images/flags/mauritania.png", "images/outline/mauritania.png" },
                new[] { "Мадагаскар", "Madagascar", "africa", "Антананариву", "Antananarivo", "images/flags/madagascar.png", "images/outline/madagascar.png" },
                new[] { "Малави", "Malawi", "africa", "Лилонгве", "Lilongwe", "images/flags/malawi.png", "images/outline/malawi.png" },
                new[] { "Малайзия", "Malaysia", "asia", "Куала-Лумпур", "Kuala Lumpur", "images/flags/malaysia.png", "images/outline/malaysia.png" },
                new[] { "Мали", "Mali", "africa", "Бамако", "Bamako", "images/flags/mali.png", "images/outline/mali.png" },
                new[] { "Мальдивы", "Maldives", "asia", "Мале", "Malé", "images/flags/maldives.png", "images/outline/maldives.png" },
                new[] { "Мальта", "Malta", "europe", "Валлетта", "Valletta", "images/flags/malta.png", "images/outline/malta.png" },
                new[] { "Марокко", "Morocco", "africa", "Рабат", "Rabat", "images/flags/morocco.png", "images/outline/morocco.png" },
                new[] { "Маршалловы Острова", "Marshall Islands", "oceania", "Маджуро", "Majuro", "images/flags/marshall_islands.png", "images/outline/marshall_islands.png" },
                new[] { "Мексика", "Mexico", "america", "Мехико", "Mexico City", "images/flags/mexico.png", "images/outline/mexico.png" },
                new[] { "Микронезия", "Micronesia", "oceania", "Паликир", "Palikir", "images/flags/micronesia.png", "images/outline/micronesia.png" },
                new[] { "Мозамбик", "Mozambique", "africa", "Мапуту", "Maputo", "images/flags/mozambique.png", "images/outline/mozambique.png" },
                new[] { "Молдавия", "Moldova", "europe", "Кишинёв", "Chișinău", "images/flags/moldova.png", "images/outline/moldova.png" },
                new[] { "Монако", "Monaco", "europe", "Монако", "Monaco", "images/flags/monaco.png", "images/outline/monaco.png" },
                new[] { "Монголия", "Mongolia", "asia", "Улан-Батор", "Ulaanbaatar", "images/flags/mongolia.png", "images/outline/mongolia.png" },
                new[] { "Мьянма", "Myanmar", "asia", "Нейпьидо", "Naypyidaw", "images/flags/myanmar.png", "images/outline/myanmar.png" },
                new[] { "Намибия", "Namibia", "africa", "Виндхук", "Windhoek", "images/flags/namibia.png", "images/outline/namibia.png" },
                new[] { "Науру", "Nauru", "oceania", "Ярен", "Yaren", "images/flags/nauru.png", "images/outline/nauru.png" },
                new[] { "Непал", "Nepal", "asia", "Катманду", "Kathmandu", "images/flags/nepal.png", "images/outline/nepal.png" },
                new[] { "Нигер", "Niger", "africa", "Ниамей", "Niamey", "images/flags/niger.png", "images/outline/niger.png" },
                new[] { "Нигерия", "Nigeria", "africa", "Абуджа", "Abuja", "images/flags/nigeria.png", "images/outline/nigeria.png" },
                new[] { "Нидерланды", "Netherlands", "europe", "Амстердам", "Amsterdam", "images/flags/netherlands.png", "images/outline/netherlands.png" },
                new[] { "Никарагуа", "Nicaragua", "america", "Манагуа", "Managua", "images/flags/nicaragua.png", "images/outline/nicaragua.png" },
                new[] { "Новая Зеландия", "New Zealand", "oceania", "Веллингтон", "Wellington", "images/flags/new_zealand.png", "images/outline/new_zealand.png" },
                new[] { "Норвегия", "Norway", "europe", "Осло", "Oslo", "images/flags/norway.png", "images/outline/norway.png" },
                new[] { "Объединённые Арабские Эмираты", "United Arab Emirates", "asia", "Абу-Даби", "Abu Dhabi", "images/flags/uae.png", "images/outline/uae.png" },
                new[] { "Оман", "Oman", "asia", "Маскат", "Muscat", "images/flags/oman.png", "images/outline/oman.png" },
                new[] { "Пакистан", "Pakistan", "asia", "Исламабад", "Islamabad", "images/flags/pakistan.png", "images/outline/pakistan.png" },
                new[] { "Палау", "Palau", "oceania", "Нгерулмуд", "Ngerulmud", "images/flags/palau.png", "images/outline/palau.png" },
                new[] { "Панама", "Panama", "america", "Панама", "Panama City", "images/flags/panama.png", "images/outline/panama.png" },
                new[] { "Папуа — Новая Гвинея", "Papua New Guinea", "oceania", "Порт-Морсби", "Port Moresby", "images/flags/papua_new_guinea.png", "images/outline/papua_new_guinea.png" },
                new[] { "Палестина", "Palestine", "asia", "Рамалла", "Ramallah", "images/flags/palestine.png", "images/outline/palestine.png" },
                new[] { "Парагвай", "Paraguay", "america", "Асунсьон", "Asunción", "images/flags/paraguay.png", "images/outline/paraguay.png" },
                new[] { "Перу", "Peru", "america", "Лима", "Lima", "images/flags/peru.png", "images/outline/peru.png" },
                new[] { "Польша", "Poland", "europe", "Варшава", "Warsaw", "images/flags/poland.png", "images/outline/poland.png" },
                new[] { "Португалия", "Portugal", "europe", "Лиссабон", "Lisbon", "images/flags/portugal.png", "images/outline/portugal.png" },
                new[] { "Россия", "Russia", "europe", "Москва", "Moscow", "images/flags/russia.png", "images/outline/russia.png" },
                new[] { "Руанда", "Rwanda", "africa", "Кигали", "Kigali", "images/flags/rwanda.png", "images/outline/rwanda.png" },
                new[] { "Румыния", "Romania", "europe", "Бухарест", "Bucharest", "images/flags/romania.png", "images/outline/romania.png" },
                new[] { "Сальвадор", "El Salvador", "america", "Сан-Сальвадор", "San Salvador", "images/flags/el_salvador.png", "images/outline/el_salvador.png" },
                new[] { "Самоа", "Samoa", "oceania", "Апиа", "Apia", "images/flags/samoa.png", "images/outline/samoa.png" },
                new[] { "Сан-Марино", "San Marino", "europe", "Сан-Марино", "San Marino", "images/flags/san_marino.png", "images/outline/san_marino.png" },
                new[] { "Сан-Томе и Принсипи", "Sao Tome and Principe", "africa", "Сан-Томе", "São Tomé", "images/flags/sao_tome.png", "images/outline/sao_tome.png" },
                new[] { "Саудовская Аравия", "Saudi Arabia", "asia", "Эр-Рияд", "Riyadh", "images/flags/saudi_arabia.png", "images/outline/saudi_arabia.png" },
                new[] { "Северная Македония", "North Macedonia", "europe", "Скопье", "Skopje", "images/flags/north_macedonia.png", "images/outline/north_macedonia.png" },
                new[] { "Сейшельские Острова", "Seychelles", "africa", "Виктория", "Victoria", "images/flags/seychelles.png", "images/outline/seychelles.png" },
                new[] { "Сенегал", "Senegal", "africa", "Дакар", "Dakar", "images/flags/senegal.png", "images/outline/senegal.png" },
                new[] { "Сент-Винсент и Гренадины", "Saint Vincent and the Grenadines", "america", "Кингстаун", "Kingstown", "images/flags/st_vincent.png", "images/outline/st_vincent.png" },
                new[] { "Сент-Китс и Невис", "Saint Kitts and Nevis", "america", "Бастер", "Basseterre", "images/flags/st_kitts.png", "images/outline/st_kitts.png" },
                new[] { "Сент-Люсия", "Saint Lucia", "america", "Кастри", "Castries", "images/flags/st_lucia.png", "images/outline/st_lucia.png" },
                new[] { "Сербия", "Serbia", "europe", "Белград", "Belgrade", "images/flags/serbia.png", "images/outline/serbia.png" },
                new[] { "Сингапур", "Singapore", "asia", "Сингапур", "Singapore", "images/flags/singapore.png", "images/outline/singapore.png" },
                new[] { "Сирия", "Syria", "asia", "Дамаск", "Damascus", "images/flags/syria.png", "images/outline/syria.png" },
                new[] { "Словакия", "Slovakia", "europe", "Братислава", "Bratislava", "images/flags/slovakia.png", "images/outline/slovakia.png" },
                new[] { "Словения", "Slovenia", "europe", "Любляна", "Ljubljana", "images/flags/slovenia.png", "images/outline/slovenia.png" },
                new[] { "Соединённые Штаты Америки", "United States", "america", "Вашингтон", "Washington, D.C.", "images/flags/usa.png", "images/outline/usa.png" },
                new[] { "Соломоновы Острова", "Solomon Islands", "oceania", "Хониара", "Honiara", "images/flags/solomon_islands.png", "images/outline/solomon_islands.png" },
                new[] { "Сомали", "Somalia", "africa", "Могадишо", "Mogadishu", "images/flags/somalia.png", "images/outline/somalia.png" },
                new[] { "Судан", "Sudan", "africa", "Хартум", "Khartoum", "images/flags/sudan.png", "images/outline/sudan.png" },
                new[] { "Суринам", "Suriname", "america", "Парамарибо", "Paramaribo", "images/flags/suriname.png", "images/outline/suriname.png" },
                new[] { "Сьерра-Леоне", "Sierra Leone", "africa", "Фритаун", "Freetown", "images/flags/sierra_leone.png", "images/outline/sierra_leone.png" },
                new[] { "Таджикистан", "Tajikistan", "asia", "Душанбе", "Dushanbe", "images/flags/tajikistan.png", "images/outline/tajikistan.png" },
                new[] { "Таиланд", "Thailand", "asia", "Бангкок", "Bangkok", "images/flags/thailand.png", "images/outline/thailand.png" },
                new[] { "Танзания", "Tanzania", "africa", "Додома", "Dodoma", "images/flags/tanzania.png", "images/outline/tanzania.png" },
                new[] { "Того", "Togo", "africa", "Ломе", "Lomé", "images/flags/togo.png", "images/outline/togo.png" },
                new[] { "Тонга", "Tonga", "oceania", "Нукуалофа", "Nukuʻalofa", "images/flags/tonga.png", "images/outline/tonga.png" },
                new[] { "Тринидад и Тобаго", "Trinidad and Tobago", "america", "Порт-оф-Спейн", "Port of Spain", "images/flags/trinidad_tobago.png", "images/outline/trinidad_tobago.png" },
                new[] { "Тувалу", "Tuvalu", "oceania", "Фунафути", "Funafuti", "images/flags/tuvalu.png", "images/outline/tuvalu.png" },
                new[] { "Тунис", "Tunisia", "africa", "Тунис", "Tunis", "images/flags/tunisia.png", "images/outline/tunisia.png" },
                new[] { "Туркмения", "Turkmenistan", "asia", "Ашхабад", "Ashgabat", "images/flags/turkmenistan.png", "images/outline/turkmenistan.png" },
                new[] { "Турция", "Turkey", "asia", "Анкара", "Ankara", "images/flags/turkey.png", "images/outline/turkey.png" },
                new[] { "Уганда", "Uganda", "africa", "Кампала", "Kampala", "images/flags/uganda.png", "images/outline/uganda.png" },
                new[] { "Узбекистан", "Uzbekistan", "asia", "Ташкент", "Tashkent", "images/flags/uzbekistan.png", "images/outline/uzbekistan.png" },
                new[] { "Украина", "Ukraine", "europe", "Киев", "Kyiv", "images/flags/ukraine.png", "images/outline/ukraine.png" },
                new[] { "Уругвай", "Uruguay", "america", "Монтевидео", "Montevideo", "images/flags/uruguay.png", "images/outline/uruguay.png" },
                new[] { "Фиджи", "Fiji", "oceania", "Сува", "Suva", "images/flags/fiji.png", "images/outline/fiji.png" },
                new[] { "Филиппины", "Philippines", "asia", "Манила", "Manila", "images/flags/philippines.png", "images/outline/philippines.png" },
                new[] { "Финляндия", "Finland", "europe", "Хельсинки", "Helsinki", "images/flags/finland.png", "images/outline/finland.png" },
                new[] { "Франция", "France", "europe", "Париж", "Paris", "images/flags/france.png", "images/outline/france.png" },
                new[] { "Хорватия", "Croatia", "europe", "Загреб", "Zagreb", "images/flags/croatia.png", "images/outline/croatia.png" },
                new[] { "Центральноафриканская Республика", "Central African Republic", "africa", "Банги", "Bangui", "images/flags/car.png", "images/outline/car.png" },
                new[] { "Чад", "Chad", "africa", "Нджамена", "N'Djamena", "images/flags/chad.png", "images/outline/chad.png" },
                new[] { "Черногория", "Montenegro", "europe", "Подгорица", "Podgorica", "images/flags/montenegro.png", "images/outline/montenegro.png" },
                new[] { "Чехия", "Czech Republic", "europe", "Прага", "Prague", "images/flags/czech_republic.png", "images/outline/czech_republic.png" },
                new[] { "Чили", "Chile", "america", "Сантьяго", "Santiago", "images/flags/chile.png", "images/outline/chile.png" },
                new[] { "Швейцария", "Switzerland", "europe", "Берн", "Bern", "images/flags/switzerland.png", "images/outline/switzerland.png" },
                new[] { "Швеция", "Sweden", "europe", "Стокгольм", "Stockholm", "images/flags/sweden.png", "images/outline/sweden.png" },
                new[] { "Шри-Ланка", "Sri Lanka", "asia", "Шри-Джаяварденепура-Котте", "Sri Jayawardenepura Kotte", "images/flags/sri_lanka.png", "images/outline/sri_lanka.png" },
                new[] { "Эквадор", "Ecuador", "america", "Кито", "Quito", "images/flags/ecuador.png", "images/outline/ecuador.png" },
                new[] { "Экваториальная Гвинея", "Equatorial Guinea", "africa", "Малабо", "Malabo", "images/flags/equatorial_guinea.png", "images/outline/equatorial_guinea.png" },
                new[] { "Эритрея", "Eritrea", "africa", "Асмэра", "Asmara", "images/flags/eritrea.png", "images/outline/eritrea.png" },
                new[] { "Эсватини", "Eswatini", "africa", "Мбабане", "Mbabane", "images/flags/eswatini.png", "images/outline/eswatini.png" },
                new[] { "Эстония", "Estonia", "europe", "Таллин", "Tallinn", "images/flags/estonia.png", "images/outline/estonia.png" },
                new[] { "Эфиопия", "Ethiopia", "africa", "Аддис-Абеба", "Addis Ababa", "images/flags/ethiopia.png", "images/outline/ethiopia.png" },
                new[] { "Южно-Африканская Республика", "South Africa", "africa", "Претория", "Pretoria", "images/flags/south_africa.png", "images/outline/south_africa.png" },
                new[] { "Южный Судан", "South Sudan", "africa", "Джуба", "Juba", "images/flags/south_sudan.png", "images/outline/south_sudan.png" },
                new[] { "Ямайка", "Jamaica", "america", "Кингстон", "Kingston", "images/flags/jamaica.png", "images/outline/jamaica.png" },
                new[] { "Япония", "Japan", "asia", "Токио", "Tokyo", "images/flags/japan.png", "images/outline/japan.png" }
            };

            var countries = rawCountries.Select(c =>
            {
                var id = NormalizeId(c[1]);

                return new Country
                {
                    Id = id,
                    Name = new LocalizedText
                    {
                        Ru = c[0],
                        En = c[1]
                    },
                    Capital = new LocalizedText
                    {
                        Ru = c[3],
                        En = c[4]
                    },
                    Region = c[2],
                    FlagImage = c[5],
                    OutlineImage = c[6],
                    LanguageAudio = $"sounds/languages/{id}.mp3"
                };
            }).ToList();

            await _countries.InsertManyAsync(countries);
        }

        public async Task SeedQuestionsAsync()
        {
            //var collection = _context.GetCollection<Question>("questions");

            if (await _questions.CountDocumentsAsync(_ => true) > 0)
                return;

            var countries = await _context
                .GetCollection<Country>("countries")
                .Find(_ => true)
                .ToListAsync();

            var questions = new List<Question>();

            foreach (var country in countries)
            {
                questions.Add(new Question
                {
                    Id = $"{country.Id}_capital",
                    CountryId = country.Id,
                    Type = GameMode.Capital,
                    Difficulty = 1
                });

                questions.Add(new Question
                {
                    Id = $"{country.Id}_flag",
                    CountryId = country.Id,
                    Type = GameMode.Flag,
                    Difficulty = 1
                });

                questions.Add(new Question
                {
                    Id = $"{country.Id}_outline",
                    CountryId = country.Id,
                    Type = GameMode.Outline,
                    Difficulty = 2
                });

                questions.Add(new Question
                {
                    Id = $"{country.Id}_language",
                    CountryId = country.Id,
                    Type = GameMode.Language,
                    Difficulty = 2
                });
            }

            await _questions.InsertManyAsync(questions);
        }

        private static string NormalizeId(string text)
        {
            text = text.ToLowerInvariant();
            text = text.Replace(" ", "_");

            var normalized = text.Normalize(NormalizationForm.FormD);
            var sb = new StringBuilder();

            foreach (var ch in normalized)
            {
                var uc = Char.GetUnicodeCategory(ch);
                if (uc != UnicodeCategory.NonSpacingMark)
                    sb.Append(ch);
            }

            return sb.ToString();
        }
    }
}
