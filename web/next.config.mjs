/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone', // smaller Docker image: only the files actually needed
};

export default nextConfig;
