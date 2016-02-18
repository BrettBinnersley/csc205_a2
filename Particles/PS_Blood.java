/* Particle System.
Brett Binnersley, V00776751

Particle System representing a blood splatter.
*/

class PS_Blood extends ParticleSystem {
  public PS_Blood(double x, double y) {
    super((int)x, (int)y, -2);
    int intx = (int)x;
    int inty = (int)y;

    for (int i=0; i< 10; ++i) {
      AddParticle(new Particle_Blood(intx, inty));
    }
  }

  public void ParticleSystemStep() {
    if (particles.isEmpty()) {
      Destroy(); // Only keep the PS alive as long as there is blood on the screen.
    }
  }
}
