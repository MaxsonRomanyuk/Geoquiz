using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace GeoQuiz_backend.Controllers
{
    [ApiController]
    [Route("api/test")]
    public class TestController : ControllerBase
    {
        [HttpPost("echo")]
        public IActionResult Echo([FromBody] TestRequest request)
        {
            Console.WriteLine($"Received from Android: {request.Message}");
            var response = new TestResponse
            {
                Message = "Hello from .NET",
                Received = request.Message
            };

            return Ok(response);
        }

    }

    public class TestRequest
    {
        public string Message { get; set; }
    }

    public class TestResponse
    {
        public string Message { get; set; }
        public string Received { get; set; }
    }
}
