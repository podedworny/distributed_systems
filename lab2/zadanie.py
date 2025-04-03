from fastapi import FastAPI, HTTPException, Security, Depends
import aiohttp
from fastapi.params import Query
from fastapi.security import APIKeyHeader
from starlette.staticfiles import StaticFiles
from pydantic import BaseModel, Field
import asyncio
import os
from dotenv import load_dotenv

steam_api_1 = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/" # id konta steam
steam_api_2 = "https://store.steampowered.com/api/appdetails" #informacje o grze na steamie
steam_api_3 = "https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/" #liczba aktywnych graczy
steam_api_4 = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/" # informacje o koncie steam
steam_api_5 = "https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/" # informacje o grach na koncie steam

twitch_api_1 = "https://api.twitch.tv/helix/games" # informacje o grze na twitchu
twitch_api_2 = "https://api.twitch.tv/helix/streams" # informacje o streamach danej gry

# wczytanie kluczy api
load_dotenv()

API_KEY = os.getenv("API_KEY")
twitch_api_key = os.getenv("TWITCH_API_KEY")
client_id = os.getenv("TWITCH_CLIENT_ID")
steam_api_key = os.getenv("STEAM_API_KEY")
rawg_api_key = os.getenv("RAWG_API_KEY")


games_names_exceptions = {"Counter-Strike 2": "Counter-Strike"}

app = FastAPI()
api_key_header = APIKeyHeader(name="X-API-Key")
app.mount("/static", StaticFiles(directory="static"), name="static")

async def get_api_key(api_key: str = Security(api_key_header)):
    if api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Niepoprawny klucz API")
    return api_key

class SteamQueryParams(BaseModel):
    nick_name: str = Field(..., min_length=2, max_length=50, pattern=r'^[a-zA-Z0-9_-]+$', description="Nazwa konta Steam")

async def fetch_json(session, url, headers=None, params=None):
    try:
        async with session.get(url, headers=headers, params=params, timeout=10) as response:
            if response.status == 200:
                return await response.json()
            else:
                return {"error": f"API returned status code {response.status}"}
    except asyncio.TimeoutError:
        return {"error": "Request timed out"}
    except Exception as e:
        return {"error": f"An error occurred: {str(e)}"}

@app.get("/steam_info")
async def steam_account_info(params: SteamQueryParams = Query(...), api_key: str = Depends(get_api_key)):
    # pobranie id konta steam
    nick_name = params.nick_name
    async with aiohttp.ClientSession() as session:
        get_account_id = await fetch_json(
            session,
            steam_api_1 + f"?key={steam_api_key}&vanityurl={nick_name}"
        )

        # sprawdzenie czy konto istnieje
        if get_account_id["response"]["success"] != 1:
            raise HTTPException(status_code=404, detail="User not found")

        account_id = get_account_id["response"]["steamid"]

        # pobranie informacji o koncie
        get_account_info = await fetch_json(
            session,
            steam_api_4 + f"?key={steam_api_key}&steamids={account_id}"
        )
        account_info = get_account_info["response"]["players"][0]

        # dodanie informacji o koncie do słownika
        user_info = {
            "personname": account_info["personaname"],
            "profileurl": account_info["profileurl"],
            "avatar": account_info["avatarmedium"]
        }

        # pobranie informacji o grach na koncie
        get_games = await fetch_json(
            session,
            steam_api_5 + f"?key={steam_api_key}&steamid={account_id}"
        )

        # sprawdzenie czy konto posiada jakieś gry
        if not get_games["response"]:
            return {"user_info": user_info, "error": "No games found for this user"}

        # liczba granych gier
        games_count = get_games["response"]["total_count"]

        games = []
        livestreams = {}
        rating = {}

        # pobranie informacji o grach na steamie
        headers = {
            "Authorization" : f"Bearer {twitch_api_key}",
            "Client-Id" : client_id
        }

        game_tasks = []

        for i in range(min(games_count, 3)):
            # pobranie informacji o grze
            game = get_games["response"]["games"][i]
            game_tasks.append(process_game(session, game, headers))

        game_results = await asyncio.gather(*game_tasks)

        for result in game_results:
            game_info, game_livestream, game_rating = result
            games.append(game_info)
            livestreams[game_info["name"]] = game_livestream
            rating[game_info["name"]] = game_rating


    return {
        "games": games,
        "livestreams": livestreams,
        "user_info": user_info,
        "rating": rating
    }

async def process_game(session, game, headers):
    game_info = {
        "name": game["name"],
        "playtime_forever": game["playtime_forever"],
        "playtime_2weeks": game["playtime_2weeks"]
    }

    tasks = [
        fetch_json(
            session, steam_api_3 + f"?appid={game["appid"]}"
        ),
        fetch_json(
            session, steam_api_2 + f"?appids={game["appid"]}"
        )
    ]

    current_players_response, steam_game_info_response = await asyncio.gather(*tasks)

    # pobranie liczby aktywnych graczy
    if current_players_response["response"]["result"] == 1:
        game_info["current_players"] = current_players_response["response"]["player_count"]
    else:
        game_info["current_players"] = "N/A"

    # pobranie informacji o cenie gry
    steam_game_info = steam_game_info_response[str(game["appid"])]

    if steam_game_info["success"]:
        if steam_game_info["data"]["is_free"]:
            game_info["price"] = 0.0
        else:
            game_info["price"] = steam_game_info["data"]["price_overview"]["final"]
        game_info["link"] = "https://store.steampowered.com/app/" + str(game["appid"])

    # obłsuga wyjątków w nazwach gier
    if game["name"] in games_names_exceptions:
        game_name = games_names_exceptions[game["name"]]
    else:
        game_name = game["name"]

    new_tasks = [
        get_twitch_streams(
            session, game_name, headers
        ),
        get_game_rating(
            session, game["name"]
        )
    ]

    game_livestream, game_rating = await asyncio.gather(*new_tasks)

    return game_info, game_livestream, game_rating


async def get_twitch_streams(session, game_name, headers):
    # pobranie informacji o grze na twitchu
    params = {
        "name": game_name
    }

    response = await fetch_json(session, twitch_api_1, headers=headers, params=params)

    if response["data"]:
        game_id = response["data"][0]["id"]

        params = {
            "game_id": game_id,
            "type": "live"
        }

        # pobranie informacji o streamach danej gry
        get_streams = await fetch_json(session, twitch_api_2, headers=headers, params=params)
        streams = get_streams["data"]

        if streams:
            # sortowanie streamów po liczbie widzów
            streams.sort(key=lambda stream: stream["viewer_count"], reverse=True)
            return {
                "success": True,
                "user_name": streams[0]["user_name"],
                "viewer_count": streams[0]["viewer_count"],
                "thumbnail_url": streams[0]["thumbnail_url"],
                "link": "https://www.twitch.tv/" + streams[0]["user_name"]

            }
    return {
        "success": False
    }


async def get_game_rating(session, game_name):
    # pobranie informacji o ocenach gry
    url = f"https://api.rawg.io/api/games?search={game_name}&key={rawg_api_key}"

    current_rating = {}
    summary = 0
    count = 0

    response = await fetch_json(session, url)

    if response["count"] > 0:
        ratings = response["results"][0]["ratings"]

        for r in ratings:
            summary += int(r["id"]) * int(r["count"])
            count += int(r["count"])
            current_rating[r["title"]] = {
                "percent": r["percent"],
                "count": r["count"]
            }
        if count > 0:
            g = summary / count
            if 4 < g <= 5:
                current_rating["summary"] = "Wyjątkowa"
            elif 3 <= g < 4:
                current_rating["summary"] = "Do polecenia"
            elif 2 <= g < 3:
                current_rating["summary"] = "Taka sobie"
            elif 1 <= g < 2:
                current_rating["summary"] = "Do pominięcia"
        else:
            current_rating["summary"] = "Brak ocen"

    else:
        current_rating[game_name] = {
            "exceptional": {
                "percent": 0,
                "count": 0
            },
            "recommended": {
                "percent": 0,
                "count": 0
            },
            "meh": {
                "percent": 0,
                "count": 0
            },
            "skip": {
                "percent": 0,
                "count": 0
            },
            "summary": "Brak ocen"
        }
    return current_rating