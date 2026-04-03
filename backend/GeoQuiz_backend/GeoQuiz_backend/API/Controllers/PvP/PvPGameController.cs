using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Application.DTOs.PvP;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.API.Controllers.PvP
{
    [ApiController]
    [Route("api/pvp/game")]
    [Authorize]
    public class PvPGameController : ControllerBase
    {
        private readonly IPvPGameSessionService _service;

        public PvPGameController(IPvPGameSessionService service)
        {
            _service = service;
        }

        [HttpPost("{matchId}/start")]
        public async Task<IActionResult> Start(Guid matchId)
        {
            await _service.StartMatchAsync(matchId);
            return Ok();
        }

        [HttpGet("{matchId}/state")]
        public async Task<IActionResult> State(Guid matchId)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );
            return Ok(await _service.GetGameStateAsync(matchId, userId));
        }

        [HttpPost("{matchId}/answer")]
        public async Task<IActionResult> Answer(Guid matchId, [FromBody] SubmitAnswerRequest req)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );
            return Ok(await _service.SubmitAnswerAsync(matchId, userId, req));
        }

        [HttpPost("{matchId}/finish")]
        public async Task<IActionResult> Finish(Guid matchId)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );
            //
            return Ok();
        }
    }
}
