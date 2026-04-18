using System.Runtime.Serialization;

namespace GeoQuiz_backend.Domain.Enums
{
    public enum Region
    {
        [EnumMember(Value = "europe")]
        Europe = 1,
        [EnumMember(Value = "asia")]
        Asia = 2,
        [EnumMember(Value = "africa")]
        Africa = 3,
        [EnumMember(Value = "america")]
        America = 4,
        [EnumMember(Value = "oceania")]
        Oceania = 5
    }
}
