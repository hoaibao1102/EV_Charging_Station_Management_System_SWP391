import { useContext } from "react";
import { PathContext } from "../context/PathContext.js";

const usePaths = () => {
  const ctx = useContext(PathContext);
  if (!ctx) throw new Error("usePaths must be used within <PathProvider>");
  return ctx;
};

export default usePaths;