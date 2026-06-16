using DmsApi.Models;
using Microsoft.EntityFrameworkCore;

namespace DmsApi.Data;

/// <summary>
/// Primary Oracle DB context. Schema: DMSINHOUSE
/// </summary>
public class DmsDbContext : DbContext
{
    public DmsDbContext(DbContextOptions<DmsDbContext> options) : base(options) { }

    public DbSet<BatchDetails>        BatchDetails      { get; set; } = null!;
    public DbSet<DmsGeneralParameter> GeneralParameters { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.HasDefaultSchema("DMSINHOUSE");

        // BatchDetails composite PK
        modelBuilder.Entity<BatchDetails>()
            .HasKey(b => new
            {
                b.CompanyId,
                b.LocationId,
                b.SubApplicationName,
                b.BatchNumber,
                b.ProductCode,
            });

        // DmsGeneralParameter single PK
        modelBuilder.Entity<DmsGeneralParameter>()
            .HasKey(p => p.ParameterId);
    }
}
