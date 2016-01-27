using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Data.Entity;

namespace IoTREST.Models
{
    /// <summary>
    /// Class for representing a single measurement from a pulse oximeter following the IEEE 11073 standard.
    /// </summary>
    public class PulseOximetryMeasurement
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int Id { get; set; }
        
        public float HeartRate { get; set; }
        
        public string HeartRateUnit { get; set; }
        
        public float BloodOxygenSaturation { get; set; }
        
        public string BloodOxygenSaturationUnit { get; set; }
        
        public DateTime TimeStamp { get; set; }
        
        public string PatientIdentification { get; set; }
    }

    public class OximetryContext : DbContext
    {
        public DbSet<PulseOximetryMeasurement> PulseOximetryMeasurements { get; set; } 
    }
}