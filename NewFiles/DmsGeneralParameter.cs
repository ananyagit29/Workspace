using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DmsApi.Models;

/// <summary>
/// Maps to DMSINHOUSE.DMS_GENERAL_PARAMETERS Oracle table.
/// Stores application-level config like year range for dropdowns.
/// </summary>
[Table("DMS_GENERAL_PARAMETERS")]
public class DmsGeneralParameter
{
    [Key]
    [Column("PARAMETER_ID")]
    public long ParameterId { get; set; }

    [Column("APPLICATION_NAME")]
    [MaxLength(100)]
    public string ApplicationName { get; set; } = null!;

    [Column("PARAMETER_NAME")]
    [MaxLength(100)]
    public string ParameterName { get; set; } = null!;

    [Column("PARAMETER_VALUE")]
    [MaxLength(100)]
    public string ParameterValue { get; set; } = null!;

    [Column("REQUIRED")]
    [MaxLength(10)]
    public string? Required { get; set; }

    [Column("LOCATION")]
    [MaxLength(20)]
    public string? Location { get; set; }
}
