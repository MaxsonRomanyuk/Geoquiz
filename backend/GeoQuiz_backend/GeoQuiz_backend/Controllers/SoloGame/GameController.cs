using GeoQuiz_backend.Application.Services;
using GeoQuiz_backend.DTOs.Game;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.Controllers.SoloGame
{
    [ApiController]
    [Route("api/game")]
    public class GameController : ControllerBase
    {
        private readonly GameService _gameService;

        public GameController(GameService gameService)
        {
            _gameService = gameService;
        }

        [Authorize]
        [HttpPost("finish")]
        public async Task<IActionResult> FinishGame([FromBody] FinishGameRequest request)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );

            await _gameService.ProcessFinishedGameAsync(userId, request);
            return Ok();
        }

        [Authorize]
        [HttpPost("sync")]
        public async Task<IActionResult> SyncGames([FromBody] List<SyncGameSessionRequest> games)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );

            await _gameService.SyncGamesAsync(userId, games);
            return Ok();
        }
    }
}
