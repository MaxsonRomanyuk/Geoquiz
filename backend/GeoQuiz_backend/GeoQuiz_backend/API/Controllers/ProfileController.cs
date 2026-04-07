using GeoQuiz_backend.Application.DTOs.User;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.API.Controllers
{
    [ApiController]
    [Route("api/profile")]
    [Authorize]
    public class ProfileController : ControllerBase
    {
        private readonly IProfileService _profileService;
        public ProfileController(IProfileService profileService)
        {
            _profileService = profileService;
        }
        [HttpGet("me")]
        public async Task<IActionResult> GetProfile()
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );
            var profile = await _profileService.GetProfile(userId);

            if (profile == null)
                return NotFound();

            return Ok(profile);
        }
        [HttpPut("update")]
        public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );
            await _profileService.UpdateProfile(userId, request);

            return Ok(new { Message = "Profile updated successfully" });
        }
    }
}
