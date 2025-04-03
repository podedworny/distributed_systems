document.getElementById("submit").addEventListener("click", findPlayer);

function findPlayer(event) {
    event.preventDefault();

    // Czyszczenie poprzednich wyników
    const gameDivs = [
        document.getElementById("game-1"),
        document.getElementById("game-2"),
        document.getElementById("game-3")
    ];
    gameDivs.forEach(gameDiv => gameDiv.innerHTML = "");

    const nick_name = document.getElementById("nick-name").value;

    // Czyszczenie poprzednich danych użytkownika
    document.getElementById("profile-name").innerHTML = "";
    document.getElementById("profile-image").innerHTML = "";
    document.getElementById("profile-url").innerHTML = "";
    document.getElementById("no-user").innerHTML = "";
    document.getElementById("game-name-1").innerHTML = "";
    document.getElementById("game-name-2").innerHTML = "";
    document.getElementById("game-name-3").innerHTML = "";

    // Wysyłanie żądania do serwera z kluczem API
    fetch('/steam_info?nick_name=' + nick_name, {
        method: 'GET',
        headers: {
            'X-API-Key': 'super-tajny-klucz'  // Podmień na swój klucz API
        }
    })
        .then(res => {
            if (!res.ok) {
                if (res.status === 404) {
                    document.getElementById("no-user").innerText = "Nie ma takiego użytkownika";
                    throw new Error("Nie ma użytkownika o podanej nazwie");
                } else if (res.status === 422) {
                    document.getElementById("no-user").innerText = "Podałeś nieprawidłową nazwę użytkownika";
                    throw new Error("Nie podano nazwy użytkownika");
                } else {
                    document.getElementById("no-user").innerText = "Wystąpił błąd podczas pobierania danych";
                    throw new Error("Wystąpił błąd podczas pobierania danych");
                }
            }
            return res.json();
        })
        .then(data => {
            document.getElementById("profile-name").innerHTML = data.user_info.personname;
            document.getElementById("profile-image").innerHTML = `<img src="${data.user_info.avatar}" alt="Profile Image">`;
            document.getElementById("profile-url").innerHTML = `<a href="${data.user_info.profileurl}" target="_blank">${data.user_info.profileurl}</a>`;

            if (data.error) {
                document.getElementById("no-user").innerHTML = "Nie można załadować gier użytkownika";
                return;
            }

            if (data.games.length === 0) {
                document.getElementById("no-user").innerHTML = "Użytkownik nie grał w żadne gry w przeciągu ostatnich 2 tygodni";
                return;
            }

            data.games.forEach((game, index) => {
                if (index < 3) {
                    document.getElementById(`game-name-${index + 1}`).innerHTML = '<strong>' + game.name + '</strong>';
                    const gameDiv = document.getElementById(`game-${index + 1}`);
                    if (gameDiv) {
                        const stream = data.livestreams[game.name];
                        const rating = data.rating[game.name];

                        gameDiv.innerHTML = `
                        <p>Czas gry w 2 tygodnie: ${game.playtime_2weeks ? game.playtime_2weeks + ' minut' : 'Brak danych'}</p>
                        <p>Czas gry ogółem: ${game.playtime_forever} minut</p>
                        <p>Aktualni gracze na platformie Steam: ${game.current_players}</p>
                        <p>Aktualna cena na platformie Steam: ${(game.price / 100).toFixed(2)} PLN</p>
                        <p>Sklep Steam: <a href="${game.link}" target="_blank">${game.link}</a></p>
                        <p><strong>Oceny graczy:</strong></p>
                        <p>Wyjątkowa: ${rating["exceptional"] ? rating["exceptional"].percent + '%' : 'Brak danych'} Głosów: ${rating["exceptional"] ? rating["exceptional"].count : '0'}</p>
                        <p>Do polecenia: ${rating["recommended"] ? rating["recommended"].percent + '%' : 'Brak danych'} Głosów: ${rating["recommended"] ? rating["recommended"].count : '0'}</p>
                        <p>Taka sobie: ${rating["meh"] ? rating["meh"].percent + '%' : 'Brak danych'} Głosów: ${rating["meh"] ? rating["meh"].count : '0'}</p>
                        <p>Do pominięcia: ${rating["skip"] ? rating["skip"].percent + '%' : 'Brak danych'} Głosów: ${rating["skip"] ? rating["skip"].count : '0'}</p>
                        <p>Ocena ogólna: ${rating.summary ? rating.summary : 'Brak ocen'}</p>
                        ${stream.success ? `
                            <p><strong>Popularny stream:</strong> <a href="${stream.link}" target="_blank">${stream.user_name}</a> (${stream.viewer_count} widzów)</p>
                            <img src="${stream.thumbnail_url.replace("{width}x{height}", "320x200")}" alt="Stream thumbnail"/>
                        ` : "<p>Brak dostępnych streamów z tej gry.</p>"}
                    `;
                    }
                }
            });
        })
        .catch(error => console.error("Błąd pobierania danych:", error));
}
