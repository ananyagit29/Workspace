using DmsApi.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace DmsApi.Controllers;

/// <summary>
/// Provides application configuration from DMS_GENERAL_PARAMETERS.
/// No auth required — config data is not sensitive.
/// </summary>
[ApiController]
[Route("dmsApi/config")]
public class ConfigController : ControllerBase
{
    private readonly DmsDbContext _db;

    public ConfigController(DmsDbContext db) => _db = db;

    /// <summary>
    /// Returns applications that require a Year dropdown with their start year.
    /// Source: DMS_GENERAL_PARAMETERS where PARAMETER_NAME = 'Year'
    /// GET /dmsApi/config/yearParameters
    /// </summary>
    [HttpGet("yearParameters")]
    public async Task<IActionResult> GetYearParameters()
    {
        var rows = await _db.GeneralParameters
            .Where(p => p.ParameterName == "Year")
            .Select(p => new
            {
                applicationName = p.ApplicationName,
                parameterValue  = p.ParameterValue,
            })
            .ToListAsync();

        var result = rows
            .Select(r => new
            {
                r.applicationName,
                startYear = int.TryParse(r.parameterValue, out var y) ? y : (int?)null,
            })
            .Where(r => r.startYear.HasValue)
            .Select(r => new
            {
                r.applicationName,
                startYear = r.startYear!.Value,
            })
            .OrderBy(r => r.applicationName)
            .ToList();

        return Ok(result);
    }
}
