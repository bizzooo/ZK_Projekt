const particles = Array.from({ length: 18 }, (_, index) => index);

export function AnimatedHomepageBackground() {
  return (
    <div aria-hidden="true" className="public-home-bg">
      <div className="public-home-bg-glow" />
      <div className="public-home-bg-radar">
        <span />
        <span />
        <span />
      </div>
      <div className="public-home-bg-grid" />
      <div className="public-home-bg-scanlines" />
      <div className="public-home-bg-diagonal" />
      <div className="public-home-bg-particles">
        {particles.map((particle) => (
          <span key={particle} />
        ))}
      </div>
    </div>
  );
}
