using System.Runtime.Serialization;
using System.Text.Json.Serialization;

namespace GeoQuiz_backend.Domain.Enums
{
    [JsonConverter(typeof(JsonStringEnumConverter))]
    public enum GameMode
    {
        [EnumMember(Value = "capitals")]
        Capital = 1,
        [EnumMember(Value = "flags")]
        Flag = 2,
        [EnumMember(Value = "outlines")]
        Outline = 3,
        [EnumMember(Value = "languages")]
        Language = 4
    }
}
